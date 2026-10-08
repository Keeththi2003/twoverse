-- Profiles (FR-PRO): full and short names, pronouns, an optional phone number with sharing
-- choices, and a private nickname each user keeps for their partner.
--   * the partner never reads phone or email unless shared: those come only from
--     get_partner_profile(), and clients can't select the phone or sharing columns at all
--   * users edit only their own profile (existing "profiles: update own" policy)
--   * partner_nicknames are visible only to the user who set them
--   * pushes carry, for each recipient, what they call their partner and that partner's pronouns
--
-- Errors: not_authenticated, not_paired, invalid_nickname

-- ---------------------------------------------------------------------------
-- Names and pronouns (FR-PRO-1, FR-PRO-2)
-- ---------------------------------------------------------------------------
alter table public.profiles rename column display_name to full_name;

create type public.pronouns as enum ('she', 'he', 'they');

alter table public.profiles
    add column short_name text,
    add column pronouns public.pronouns,
    add column phone text,
    add column share_email boolean not null default false,
    add column share_phone boolean not null default false;

-- What someone likes to be called defaults to the first word of their full name.
create function private.default_short_name(full_name text)
returns text
language sql
immutable
set search_path = ''
as $$
    select left((regexp_split_to_array(btrim(full_name), '\s+'))[1], 30);
$$;

update public.profiles set short_name = private.default_short_name(full_name);

alter table public.profiles
    alter column short_name set not null,
    add constraint profiles_short_name_length check (char_length(btrim(short_name)) between 1 and 30),
    -- International format with the country code, e.g. +94771234567 (FR-PRO-4).
    add constraint profiles_phone_format check (phone is null or phone ~ '^\+[1-9][0-9]{6,14}$');

-- Profile on sign-up: the full name from the sign-up form (full_name, or display_name from older
-- app versions) or Google (full_name / name), else the email's local part; the short name from the
-- form if given, else the first word of the full name.
create or replace function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    name text;
    short text;
begin
    name := left(coalesce(
        nullif(btrim(new.raw_user_meta_data ->> 'full_name'), ''),
        nullif(btrim(new.raw_user_meta_data ->> 'display_name'), ''),
        nullif(btrim(new.raw_user_meta_data ->> 'name'), ''),
        nullif(split_part(coalesce(new.email, ''), '@', 1), ''),
        'Twoverse user'
    ), 50);
    short := coalesce(
        nullif(left(btrim(new.raw_user_meta_data ->> 'short_name'), 30), ''),
        private.default_short_name(name)
    );
    insert into public.profiles (id, full_name, short_name) values (new.id, name, short);
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- Column access (FR-PRO-6). The partner can read a profile row (names, pronouns, time zone), but
-- the phone and sharing choices are not selectable by any client; the owner reads them through
-- get_my_profile() and the partner, when shared, through get_partner_profile().
-- ---------------------------------------------------------------------------
revoke select, update on public.profiles from authenticated;
grant select (id, full_name, short_name, pronouns, distance_unit, appearance, time_zone, lock_ours, created_at, updated_at)
    on public.profiles to authenticated;
grant update (full_name, short_name, pronouns, phone, share_email, share_phone, distance_unit, appearance, time_zone, lock_ours)
    on public.profiles to authenticated;

create function public.get_my_profile()
returns table (
    full_name text,
    short_name text,
    pronouns public.pronouns,
    phone text,
    share_email boolean,
    share_phone boolean
)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
begin
    return query
    select p.full_name, p.short_name, p.pronouns, p.phone, p.share_email, p.share_phone
    from public.profiles p
    where p.id = me;
end;
$$;

-- ---------------------------------------------------------------------------
-- partner_nicknames: what a user calls their partner (FR-PRO-1). Only the user who set it can
-- read or change it; the partner never sees it.
-- ---------------------------------------------------------------------------
create table public.partner_nicknames (
    user_id uuid not null references public.profiles (id) on delete cascade,
    partner_id uuid not null references public.profiles (id) on delete cascade,
    nickname text not null check (char_length(btrim(nickname)) between 1 and 30),
    updated_at timestamptz not null default now(),
    primary key (user_id, partner_id),
    constraint partner_nicknames_not_self check (user_id <> partner_id)
);

create index partner_nicknames_partner_idx on public.partner_nicknames (partner_id);

create trigger partner_nicknames_touch before update on public.partner_nicknames
    for each row execute function private.touch_updated_at();

alter table public.partner_nicknames enable row level security;

revoke all on public.partner_nicknames from anon, authenticated;
grant select, insert, update, delete on public.partner_nicknames to authenticated;

create policy "partner_nicknames: owner reads"
    on public.partner_nicknames for select to authenticated
    using (user_id = (select auth.uid()));

create policy "partner_nicknames: owner sets for current partner"
    on public.partner_nicknames for insert to authenticated
    with check (user_id = (select auth.uid()) and partner_id = (select private.partner_id()));

create policy "partner_nicknames: owner edits"
    on public.partner_nicknames for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()) and partner_id = (select private.partner_id()));

create policy "partner_nicknames: owner clears"
    on public.partner_nicknames for delete to authenticated
    using (user_id = (select auth.uid()));

-- Sets ([p_nickname] text) or clears (null or blank) the caller's nickname for their partner.
create function public.set_partner_nickname(p_nickname text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    partner uuid := private.partner_id();
    nickname text := nullif(btrim(coalesce(p_nickname, '')), '');
begin
    if partner is null then
        raise exception 'not_paired' using errcode = 'P0001';
    end if;
    if nickname is null then
        delete from public.partner_nicknames n where n.user_id = me and n.partner_id = partner;
        return;
    end if;
    if char_length(nickname) > 30 then
        raise exception 'invalid_nickname' using errcode = 'P0001';
    end if;
    insert into public.partner_nicknames (user_id, partner_id, nickname)
    values (me, partner, nickname)
    on conflict (user_id, partner_id) do update set nickname = excluded.nickname;
end;
$$;

-- ---------------------------------------------------------------------------
-- get_partner_profile(): the partner's names and pronouns, their email and phone only when they
-- chose to share them, and the caller's own nickname for them (FR-PRO-5). Empty when unpaired.
-- ---------------------------------------------------------------------------
create function public.get_partner_profile()
returns table (
    id uuid,
    full_name text,
    short_name text,
    pronouns public.pronouns,
    time_zone text,
    email text,
    phone text,
    nickname text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
begin
    return query
    select p.id, p.full_name, p.short_name, p.pronouns, p.time_zone,
           case when p.share_email then u.email::text end,
           case when p.share_phone then p.phone end,
           n.nickname
    from public.profiles p
    join auth.users u on u.id = p.id
    left join public.partner_nicknames n on n.user_id = me and n.partner_id = p.id
    where p.id = private.partner_id();
end;
$$;

revoke all on function public.get_my_profile() from public, anon;
revoke all on function public.set_partner_nickname(text) from public, anon;
revoke all on function public.get_partner_profile() from public, anon;
grant execute on function public.get_my_profile() to authenticated;
grant execute on function public.set_partner_nickname(text) to authenticated;
grant execute on function public.get_partner_profile() to authenticated;
revoke all on function private.default_short_name(text) from public;

-- ---------------------------------------------------------------------------
-- Pushes name the partner the way the recipient knows them (FR-NOT-7): the recipient's nickname
-- for their partner, else the partner's short name, plus the partner's pronouns. The app turns
-- these into its own text; still no photos or captions (FR-NOT-1).
-- ---------------------------------------------------------------------------
create function private.partner_label_for(recipient uuid)
returns table (name text, pronouns text)
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce(n.nickname, p.short_name), p.pronouns::text
    from public.couples c
    join public.profiles p on p.id = case when c.user_a = recipient then c.user_b else c.user_a end
    left join public.partner_nicknames n on n.user_id = recipient and n.partner_id = p.id
    where c.status = 'active' and recipient in (c.user_a, c.user_b)
    limit 1;
$$;

revoke all on function private.partner_label_for(uuid) from public;

drop function public.push_targets(uuid[]);

create function public.push_targets(p_user_ids uuid[])
returns table (user_id uuid, fcm_token text, partner_name text, partner_pronouns text)
language sql
stable
security definer
set search_path = ''
as $$
    select t.user_id, t.fcm_token, l.name, l.pronouns
    from public.device_tokens t
    left join lateral private.partner_label_for(t.user_id) l on true
    where t.user_id = any (p_user_ids);
$$;

revoke all on function public.push_targets(uuid[]) from public, anon, authenticated;
grant execute on function public.push_targets(uuid[]) to service_role;

-- Same rules as before (partner only, known kinds, rate-limited); each token now comes with how
-- the partner, as the recipient, calls the caller.
drop function public.prepare_partner_push(text);

create function public.prepare_partner_push(p_kind text)
returns table (fcm_token text, partner_name text, partner_pronouns text)
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    couple public.couples%rowtype;
    partner uuid;
    min_interval interval;
    claimed boolean;
begin
    min_interval := case p_kind
        when 'wake_up' then interval '1 minute'
        when 'partner_joined' then interval '1 day'
        when 'new_memory' then interval '10 seconds'
    end;
    if min_interval is null then
        raise exception 'not_allowed' using errcode = 'P0001';
    end if;

    select c.* into couple
    from public.couples c
    where c.status = 'active' and me in (c.user_a, c.user_b);
    if not found then
        raise exception 'not_paired' using errcode = 'P0001';
    end if;
    partner := case when couple.user_a = me then couple.user_b else couple.user_a end;

    if p_kind = 'partner_joined' and couple.connected_at < now() - interval '10 minutes' then
        raise exception 'not_allowed' using errcode = 'P0001';
    end if;

    insert into private.push_rate_limits as l (user_id, kind, last_sent_at)
    values (me, p_kind, now())
    on conflict (user_id, kind) do update
        set last_sent_at = excluded.last_sent_at
        where l.last_sent_at <= now() - min_interval
    returning true into claimed;
    if claimed is null then
        raise exception 'rate_limited' using errcode = 'P0001';
    end if;

    return query
    select t.fcm_token, l.name, l.pronouns
    from public.device_tokens t
    left join lateral private.partner_label_for(partner) l on true
    where t.user_id = partner;
end;
$$;

revoke all on function public.prepare_partner_push(text) from public, anon;
grant execute on function public.prepare_partner_push(text) to authenticated;
