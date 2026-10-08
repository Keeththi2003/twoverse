-- Our Orbit (FR-ORB): when the relationship began, and the times the couple met in person.
--   * couples.together_since is set by either partner through set_together_since() (FR-ORB-1)
--   * meetups are read and changed only by the couple, live through Realtime (FR-ORB-12)
--   * a passed reunion can be recorded as a meetup once per couple (FR-ORB-10)
--   * anniversary and day-milestone pushes at 08:00 local, via pg_cron and send-push (FR-NOT-6)
--
-- Day counts: the start date is day 1, so day N is together_since + (N - 1) days.
-- Errors: not_authenticated, not_paired, invalid_date, date_in_future

-- ---------------------------------------------------------------------------
-- Together since (FR-ORB-1). Never derived from connected_at (the pairing date).
-- ---------------------------------------------------------------------------
alter table public.couples
    add column together_since date,
    add column together_since_set_by uuid references public.profiles (id) on delete set null,
    add column together_since_set_at timestamptz;

-- Either partner sets or changes it. "In the future" means after today in the caller's time zone.
create function public.set_together_since(p_date date)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    couple_id uuid;
    my_today date;
begin
    if p_date is null then
        raise exception 'invalid_date' using errcode = 'P0001';
    end if;

    select c.id into couple_id
    from public.couples c
    where c.status = 'active' and me in (c.user_a, c.user_b)
    for update;
    if not found then
        raise exception 'not_paired' using errcode = 'P0001';
    end if;

    select (now() at time zone p.time_zone)::date into my_today from public.profiles p where p.id = me;
    if p_date > my_today then
        raise exception 'date_in_future' using errcode = 'P0001';
    end if;

    update public.couples
    set together_since = p_date,
        together_since_set_by = me,
        together_since_set_at = now()
    where id = couple_id;
end;
$$;

revoke all on function public.set_together_since(date) from public, anon;
grant execute on function public.set_together_since(date) to authenticated;

-- ---------------------------------------------------------------------------
-- meetups (FR-ORB-3)
-- ---------------------------------------------------------------------------
create table public.meetups (
    id uuid primary key default gen_random_uuid(),
    couple_id uuid not null references public.couples (id) on delete cascade,
    start_date date not null,
    end_date date,
    place text check (char_length(place) <= 100 and btrim(place) <> ''),
    note text check (char_length(note) <= 300 and btrim(note) <> ''),
    created_by uuid not null references public.profiles (id) on delete cascade,
    -- The reunion (reunions.meet_at) this meetup records, so it is recorded only once (FR-ORB-10).
    from_reunion_at timestamptz,
    created_at timestamptz not null default now(),
    constraint meetups_end_after_start check (end_date is null or end_date >= start_date)
);

create index meetups_couple_start_idx on public.meetups (couple_id, start_date desc);
create index meetups_created_by_idx on public.meetups (created_by);
create unique index meetups_one_per_reunion on public.meetups (couple_id, from_reunion_at) where from_reunion_at is not null;

alter table public.meetups enable row level security;

-- Clients change only the meetup itself; the couple, author and reunion link are set once.
revoke all on public.meetups from anon, authenticated;
grant select, delete on public.meetups to authenticated;
grant insert (couple_id, start_date, end_date, place, note, created_by, from_reunion_at) on public.meetups to authenticated;
grant update (start_date, end_date, place, note) on public.meetups to authenticated;

create policy "meetups: couple reads"
    on public.meetups for select to authenticated
    using (couple_id = (select private.active_couple_id()));

create policy "meetups: couple creates"
    on public.meetups for insert to authenticated
    with check (couple_id = (select private.active_couple_id()) and created_by = (select auth.uid()));

create policy "meetups: couple edits"
    on public.meetups for update to authenticated
    using (couple_id = (select private.active_couple_id()))
    with check (couple_id = (select private.active_couple_id()));

create policy "meetups: couple deletes"
    on public.meetups for delete to authenticated
    using (couple_id = (select private.active_couple_id()));

-- Both partners see added, edited and deleted meetups live; Realtime applies the RLS policy.
alter publication supabase_realtime add table public.meetups;

-- ---------------------------------------------------------------------------
-- Anniversary and milestone pushes (FR-NOT-6, FR-ORB-5, FR-ORB-6). At 08:00 on the day in each
-- partner's time zone, or as soon as the date is set later that day; once per person per day.
-- Uses the same Vault secrets as the reunion-day job; until they exist the job waits.
-- ---------------------------------------------------------------------------
create table private.orbit_notifications (
    couple_id uuid not null references public.couples (id) on delete cascade,
    user_id uuid not null references public.profiles (id) on delete cascade,
    occasion_date date not null,
    kind text not null check (kind in ('anniversary', 'orbit_milestone')),
    count integer not null check (count > 0),
    sent_at timestamptz not null default now(),
    primary key (couple_id, user_id, occasion_date)
);

alter table private.orbit_notifications enable row level security;

-- Who to notify now, grouped by push (kind and number), recorded so each person gets one a day.
-- An anniversary on 29 February falls on 28 February in other years: Postgres date arithmetic
-- clamps the day, like java.time's plusYears.
create function private.claim_orbit_recipients()
returns table (kind text, count integer, user_ids uuid[])
language sql
security definer
set search_path = ''
as $$
    with local_days as (
        select c.id as couple_id, p.id as user_id, c.together_since as since,
               (now() at time zone p.time_zone)::date as today
        from public.couples c
        join public.profiles p on p.id in (c.user_a, c.user_b)
        where c.status = 'active'
          and c.together_since is not null
          and (now() at time zone p.time_zone)::time >= time '08:00'
    ),
    numbered as (
        select couple_id, user_id, since, today,
               (extract(year from today) - extract(year from since))::integer as years,
               (today - since + 1) as day_number
        from local_days
        where today > since
    ),
    due as (
        select couple_id, user_id, today,
               case
                   when years >= 1 and (since + make_interval(years => years))::date = today then 'anniversary'
                   when day_number in (100, 365, 500) or (day_number >= 1000 and day_number % 1000 = 0) then 'orbit_milestone'
               end as kind,
               case
                   when years >= 1 and (since + make_interval(years => years))::date = today then years
                   else day_number
               end as count
        from numbered
    ),
    claimed as (
        insert into private.orbit_notifications (couple_id, user_id, occasion_date, kind, count)
        select couple_id, user_id, today, kind, count from due where kind is not null
        on conflict do nothing
        returning kind, count, user_id
    )
    select kind, count, array_agg(user_id order by user_id) from claimed group by kind, count;
$$;

create function private.send_orbit_notifications()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    project_url text;
    internal_secret text;
    batch record;
begin
    select decrypted_secret into project_url from vault.decrypted_secrets where name = 'project_url';
    select decrypted_secret into internal_secret from vault.decrypted_secrets where name = 'push_internal_secret';
    if project_url is null or internal_secret is null then
        return;
    end if;

    for batch in select * from private.claim_orbit_recipients() loop
        perform net.http_post(
            url := rtrim(project_url, '/') || '/functions/v1/send-push',
            headers := jsonb_build_object('Content-Type', 'application/json', 'x-internal-secret', internal_secret),
            body := jsonb_build_object('type', batch.kind, 'count', batch.count, 'user_ids', to_jsonb(batch.user_ids))
        );
    end loop;
end;
$$;

revoke all on function private.claim_orbit_recipients() from public;
revoke all on function private.send_orbit_notifications() from public;

select cron.schedule(
    'twoverse-orbit-notifications',
    '*/15 * * * *',
    'select private.send_orbit_notifications()'
);
