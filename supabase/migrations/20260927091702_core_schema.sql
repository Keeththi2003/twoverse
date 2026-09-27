-- Twoverse core schema (SRS section 5).
-- All times are timestamptz (stored in UTC). Business rules that span several rows
-- (pairing, disconnect, deletion) live in RPC functions in later migrations.

create extension if not exists pgcrypto with schema extensions;

-- Internal helpers and queues. Not exposed through the Data API (only `public` is).
create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to authenticated;

-- ---------------------------------------------------------------------------
-- profiles: one row per auth user, created by trigger on sign-up (FR-AUTH-5)
-- ---------------------------------------------------------------------------
create table public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    display_name text not null check (char_length(btrim(display_name)) between 1 and 50),
    distance_unit text not null default 'km' check (distance_unit in ('km', 'mi')),
    appearance text not null default 'system' check (appearance in ('system', 'light', 'dark')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- couples: exactly two users (BR-1). user_b is null while the code is pending.
-- Ended couples keep their data hidden for a 7-day grace period (SRS 12), during
-- which both partners must confirm to reconnect; then pg_cron deletes them.
-- ---------------------------------------------------------------------------
create table public.couples (
    id uuid primary key default gen_random_uuid(),
    user_a uuid not null references public.profiles (id) on delete cascade,
    user_b uuid references public.profiles (id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'active', 'ended')),
    created_at timestamptz not null default now(),
    connected_at timestamptz,
    ended_at timestamptz,
    ended_by uuid references public.profiles (id) on delete set null,
    purge_after timestamptz,
    reconnect_requested_by uuid references public.profiles (id) on delete set null,
    reconnect_requested_at timestamptz,
    constraint couples_distinct_users check (user_a <> user_b),
    constraint couples_pending_shape check (status <> 'pending' or (user_b is null and connected_at is null)),
    constraint couples_active_shape check (status <> 'active' or (user_b is not null and connected_at is not null and ended_at is null)),
    constraint couples_ended_shape check (status <> 'ended' or (user_b is not null and ended_at is not null and purge_after is not null)),
    constraint couples_reconnect_shape check (reconnect_requested_by is null or status = 'ended')
);

-- A user has at most one open (pending or active) couple in each column…
create unique index couples_one_open_as_a on public.couples (user_a) where status in ('pending', 'active');
create unique index couples_one_open_as_b on public.couples (user_b) where status in ('pending', 'active');
create index couples_user_b_idx on public.couples (user_b);
create index couples_purge_idx on public.couples (purge_after) where status = 'ended';

-- …and across both columns (BR-1, FR-PAIR-4). Unique indexes can't span two columns
-- of different rows, so a trigger checks it under a per-user advisory lock.
create function private.enforce_one_open_couple()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    member uuid;
begin
    if new.status not in ('pending', 'active') then
        return new;
    end if;
    foreach member in array array_remove(array[new.user_a, new.user_b], null) loop
        perform pg_advisory_xact_lock(hashtextextended(member::text, 0));
        if exists (
            select 1
            from public.couples c
            where c.id <> new.id
              and c.status in ('pending', 'active')
              and member in (c.user_a, c.user_b)
        ) then
            raise exception 'already_paired' using errcode = 'P0001';
        end if;
    end loop;
    return new;
end;
$$;

create trigger couples_one_open_couple
    before insert or update of user_a, user_b, status on public.couples
    for each row execute function private.enforce_one_open_couple();

-- ---------------------------------------------------------------------------
-- couple_codes: one-time XXXX-XXXX codes valid for 24 hours (FR-PAIR-1, BR-10)
-- ---------------------------------------------------------------------------
create table public.couple_codes (
    code text primary key check (code ~ '^[A-Z0-9]{4}-[A-Z0-9]{4}$'),
    couple_id uuid not null references public.couples (id) on delete cascade,
    created_by uuid not null references public.profiles (id) on delete cascade,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    used_at timestamptz,
    used_by uuid references public.profiles (id) on delete set null,
    constraint couple_codes_expiry_after_creation check (expires_at > created_at),
    constraint couple_codes_used_shape check ((used_at is null) = (used_by is null))
);

create index couple_codes_couple_idx on public.couple_codes (couple_id);
create index couple_codes_created_by_idx on public.couple_codes (created_by);

-- ---------------------------------------------------------------------------
-- locations: only the latest location per user, no history (FR-LOC-7, BR-4)
-- ---------------------------------------------------------------------------
create table public.locations (
    user_id uuid primary key references public.profiles (id) on delete cascade,
    lat double precision not null check (lat between -90 and 90),
    lng double precision not null check (lng between -180 and 180),
    accuracy_m real check (accuracy_m >= 0),
    precision text not null default 'approximate' check (precision in ('approximate', 'precise')),
    sharing_enabled boolean not null default false,
    updated_at timestamptz not null default now()
);

-- Approximate precision is enforced on the server too: about 1 km (FR-LOC-6).
create function private.round_approximate_location()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.precision = 'approximate' then
        new.lat := round(new.lat::numeric, 2)::double precision;
        new.lng := round(new.lng::numeric, 2)::double precision;
        new.accuracy_m := greatest(coalesce(new.accuracy_m, 0), 1000);
    end if;
    return new;
end;
$$;

create trigger locations_round_approximate
    before insert or update on public.locations
    for each row execute function private.round_approximate_location();

-- ---------------------------------------------------------------------------
-- reunions: one shared countdown per couple (FR-CNT-1, FR-CNT-2, FR-CNT-6)
-- ---------------------------------------------------------------------------
create table public.reunions (
    couple_id uuid primary key references public.couples (id) on delete cascade,
    meet_at timestamptz not null,
    has_time boolean not null default true,
    place text check (char_length(place) <= 100),
    note text check (char_length(note) <= 500),
    updated_by uuid references public.profiles (id) on delete set null,
    updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- memories: photos in Ours. Files live in storage bucket "memories" at
-- {couple_id}/... Only the sender deletes; the recipient can hide (SRS 12).
-- ---------------------------------------------------------------------------
create table public.memories (
    id uuid primary key default gen_random_uuid(),
    couple_id uuid not null references public.couples (id) on delete cascade,
    sender_id uuid not null references public.profiles (id) on delete cascade,
    storage_path text not null unique,
    caption text check (char_length(caption) <= 500),
    expires_at timestamptz,
    allow_keep boolean not null default false,
    viewed_at timestamptz,
    hidden_by_recipient_at timestamptz,
    created_at timestamptz not null default now(),
    constraint memories_path_in_couple_folder check (storage_path like couple_id::text || '/%'),
    constraint memories_expiry_after_creation check (expires_at is null or expires_at > created_at)
);

create index memories_couple_created_idx on public.memories (couple_id, created_at desc);
create index memories_sender_idx on public.memories (sender_id);
create index memories_expires_idx on public.memories (expires_at) where expires_at is not null;

-- ---------------------------------------------------------------------------
-- birthday_welcomes: a message one partner prepares for the other (FR-BDY-1..3)
-- ---------------------------------------------------------------------------
create table public.birthday_welcomes (
    couple_id uuid not null references public.couples (id) on delete cascade,
    for_user_id uuid not null references public.profiles (id) on delete cascade,
    created_by uuid not null references public.profiles (id) on delete cascade,
    message text not null check (char_length(btrim(message)) between 1 and 1000),
    photo_path text,
    show_on date,
    seen_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (couple_id, for_user_id),
    constraint birthday_welcomes_not_for_self check (for_user_id <> created_by),
    constraint birthday_welcomes_photo_in_couple_folder check (photo_path is null or photo_path like couple_id::text || '/%')
);

create index birthday_welcomes_for_user_idx on public.birthday_welcomes (for_user_id);
create index birthday_welcomes_created_by_idx on public.birthday_welcomes (created_by);

-- ---------------------------------------------------------------------------
-- device_tokens: FCM tokens, one row per device (FR-NOT)
-- ---------------------------------------------------------------------------
create table public.device_tokens (
    fcm_token text primary key check (char_length(fcm_token) between 1 and 4096),
    user_id uuid not null references public.profiles (id) on delete cascade,
    updated_at timestamptz not null default now()
);

create index device_tokens_user_idx on public.device_tokens (user_id);

-- ---------------------------------------------------------------------------
-- updated_at maintenance
-- ---------------------------------------------------------------------------
create function private.touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create trigger profiles_touch before update on public.profiles
    for each row execute function private.touch_updated_at();
create trigger locations_touch before insert or update on public.locations
    for each row execute function private.touch_updated_at();
create trigger reunions_touch before insert or update on public.reunions
    for each row execute function private.touch_updated_at();
create trigger birthday_welcomes_touch before update on public.birthday_welcomes
    for each row execute function private.touch_updated_at();
create trigger device_tokens_touch before insert or update on public.device_tokens
    for each row execute function private.touch_updated_at();

-- ---------------------------------------------------------------------------
-- Profile on sign-up. Display name comes from user metadata: `display_name` for
-- email sign-up, `full_name` / `name` from Google, else the email's local part.
-- ---------------------------------------------------------------------------
create function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    name text;
begin
    name := coalesce(
        nullif(btrim(new.raw_user_meta_data ->> 'display_name'), ''),
        nullif(btrim(new.raw_user_meta_data ->> 'full_name'), ''),
        nullif(btrim(new.raw_user_meta_data ->> 'name'), ''),
        nullif(split_part(coalesce(new.email, ''), '@', 1), ''),
        'Twoverse user'
    );
    insert into public.profiles (id, display_name) values (new.id, left(name, 50));
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function private.handle_new_user();
