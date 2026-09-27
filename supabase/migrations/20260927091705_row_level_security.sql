-- Row Level Security (NFR-SEC-1, BR-2): users only reach their own rows and their
-- own active couple's data. Multi-step changes go through RPC functions instead of
-- table writes, so couples, codes and memory updates have no write policies.

-- ---------------------------------------------------------------------------
-- Helpers. security definer so policies can look up the caller's couple without
-- recursing into the couples policies; stable so Postgres can cache per statement.
-- ---------------------------------------------------------------------------
create function private.active_couple_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select c.id
    from public.couples c
    where c.status = 'active'
      and (select auth.uid()) in (c.user_a, c.user_b)
    limit 1;
$$;

create function private.partner_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select case when c.user_a = (select auth.uid()) then c.user_b else c.user_a end
    from public.couples c
    where c.status = 'active'
      and (select auth.uid()) in (c.user_a, c.user_b)
    limit 1;
$$;

revoke all on function private.active_couple_id() from public;
revoke all on function private.partner_id() from public;
grant execute on function private.active_couple_id() to authenticated;
grant execute on function private.partner_id() to authenticated;

-- Signed-out clients get nothing; there are no anon policies either.
revoke all on all tables in schema public from anon;

alter table public.profiles enable row level security;
alter table public.couples enable row level security;
alter table public.couple_codes enable row level security;
alter table public.locations enable row level security;
alter table public.reunions enable row level security;
alter table public.memories enable row level security;
alter table public.birthday_welcomes enable row level security;
alter table public.device_tokens enable row level security;

-- profiles: read own and partner's (for "Connected" and "From Her"); edit own only.
create policy "profiles: read own and partner"
    on public.profiles for select to authenticated
    using (id = (select auth.uid()) or id = (select private.partner_id()));

create policy "profiles: update own"
    on public.profiles for update to authenticated
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

-- couples: members see their couples (pending, active, or ended during the grace
-- period); all changes go through RPC functions.
create policy "couples: members read"
    on public.couples for select to authenticated
    using ((select auth.uid()) in (user_a, user_b));

-- couple_codes: the creator can see their own codes; created and consumed by RPC.
create policy "couple_codes: creator reads"
    on public.couple_codes for select to authenticated
    using (created_by = (select auth.uid()));

-- locations: own row freely; the partner's only while they share it (FR-LOC-2).
create policy "locations: read own"
    on public.locations for select to authenticated
    using (user_id = (select auth.uid()));

create policy "locations: partner reads when shared"
    on public.locations for select to authenticated
    using (sharing_enabled and user_id = (select private.partner_id()));

create policy "locations: insert own"
    on public.locations for insert to authenticated
    with check (user_id = (select auth.uid()));

create policy "locations: update own"
    on public.locations for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

create policy "locations: delete own"
    on public.locations for delete to authenticated
    using (user_id = (select auth.uid()));

-- reunions: both members of the active couple read and edit (FR-CNT-1, FR-CNT-2).
create policy "reunions: couple reads"
    on public.reunions for select to authenticated
    using (couple_id = (select private.active_couple_id()));

create policy "reunions: couple inserts"
    on public.reunions for insert to authenticated
    with check (couple_id = (select private.active_couple_id()) and updated_by = (select auth.uid()));

create policy "reunions: couple updates"
    on public.reunions for update to authenticated
    using (couple_id = (select private.active_couple_id()))
    with check (couple_id = (select private.active_couple_id()) and updated_by = (select auth.uid()));

create policy "reunions: couple clears"
    on public.reunions for delete to authenticated
    using (couple_id = (select private.active_couple_id()));

-- memories: readable by the active couple while not expired (BR-6), except that a
-- recipient who hid a memory no longer sees it. Only the sender inserts and deletes
-- (SRS 12); viewed / hidden / keep-forever changes go through RPC functions.
create policy "memories: couple reads unexpired"
    on public.memories for select to authenticated
    using (
        couple_id = (select private.active_couple_id())
        and (expires_at is null or expires_at > now())
        and (hidden_by_recipient_at is null or sender_id = (select auth.uid()))
    );

create policy "memories: sender inserts"
    on public.memories for insert to authenticated
    with check (
        couple_id = (select private.active_couple_id())
        and sender_id = (select auth.uid())
        and (expires_at is null or expires_at > now())
        and viewed_at is null
        and hidden_by_recipient_at is null
    );

create policy "memories: sender deletes"
    on public.memories for delete to authenticated
    using (couple_id = (select private.active_couple_id()) and sender_id = (select auth.uid()));

-- birthday_welcomes: the author writes it for their partner; both can read it.
create policy "birthday_welcomes: author and recipient read"
    on public.birthday_welcomes for select to authenticated
    using (
        couple_id = (select private.active_couple_id())
        and (select auth.uid()) in (for_user_id, created_by)
    );

create policy "birthday_welcomes: author inserts"
    on public.birthday_welcomes for insert to authenticated
    with check (
        couple_id = (select private.active_couple_id())
        and created_by = (select auth.uid())
        and for_user_id = (select private.partner_id())
        and seen_at is null
    );

create policy "birthday_welcomes: author updates"
    on public.birthday_welcomes for update to authenticated
    using (couple_id = (select private.active_couple_id()) and created_by = (select auth.uid()))
    with check (
        couple_id = (select private.active_couple_id())
        and created_by = (select auth.uid())
        and for_user_id = (select private.partner_id())
    );

create policy "birthday_welcomes: author deletes"
    on public.birthday_welcomes for delete to authenticated
    using (couple_id = (select private.active_couple_id()) and created_by = (select auth.uid()));

-- device_tokens: each user manages only their own tokens.
create policy "device_tokens: read own"
    on public.device_tokens for select to authenticated
    using (user_id = (select auth.uid()));

create policy "device_tokens: insert own"
    on public.device_tokens for insert to authenticated
    with check (user_id = (select auth.uid()));

create policy "device_tokens: update own"
    on public.device_tokens for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

create policy "device_tokens: delete own"
    on public.device_tokens for delete to authenticated
    using (user_id = (select auth.uid()));
