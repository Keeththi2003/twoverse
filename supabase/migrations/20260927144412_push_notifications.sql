-- Push notifications (FR-NOT) and the wake-up ping (FR-LOC-5, FR-NOT-5).
-- The send-push Edge Function only delivers; who may notify whom is decided here:
--   * a user can only push to their own partner, per kind and rate-limited
--   * reunion-day notifications are sent by pg_cron at 08:00 in the recipient's time zone
--
-- Errors: not_authenticated, not_paired, not_allowed, rate_limited

-- ---------------------------------------------------------------------------
-- Each user's time zone, so "reunion day" means their local day (FR-CNT-6, FR-NOT-4).
-- ---------------------------------------------------------------------------
alter table public.profiles add column time_zone text not null default 'UTC';

create function private.validate_time_zone()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if not exists (select 1 from pg_catalog.pg_timezone_names where name = new.time_zone) then
        raise exception 'invalid_time_zone' using errcode = '22023';
    end if;
    return new;
end;
$$;

create trigger profiles_valid_time_zone
    before insert or update of time_zone on public.profiles
    for each row execute function private.validate_time_zone();

-- ---------------------------------------------------------------------------
-- register_device_token(token): stores the caller's FCM token. A token belongs to one
-- device, so if another account used this phone before, the token moves to the caller.
-- ---------------------------------------------------------------------------
create function public.register_device_token(p_token text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
begin
    if p_token is null or char_length(p_token) not between 1 and 4096 then
        raise exception 'invalid_token' using errcode = 'P0001';
    end if;
    insert into public.device_tokens (fcm_token, user_id)
    values (p_token, me)
    on conflict (fcm_token) do update set user_id = excluded.user_id;
end;
$$;

-- ---------------------------------------------------------------------------
-- prepare_partner_push(kind): the permission check for user-triggered pushes. Allows
-- sending only to the caller's active partner, only for known kinds, rate-limited per
-- kind; returns the partner's device tokens.
--   wake_up         silent location refresh when the app opens, once a minute (FR-LOC-5)
--   partner_joined  once, within 10 minutes of pairing (FR-NOT-2)
--   new_memory      at most every 10 seconds (FR-NOT-1)
-- ---------------------------------------------------------------------------
create table private.push_rate_limits (
    user_id uuid not null references public.profiles (id) on delete cascade,
    kind text not null,
    last_sent_at timestamptz not null,
    primary key (user_id, kind)
);

create function public.prepare_partner_push(p_kind text)
returns setof text
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

    return query select t.fcm_token from public.device_tokens t where t.user_id = partner;
end;
$$;

-- ---------------------------------------------------------------------------
-- Service-role helpers for the Edge Function and the scheduled job.
-- ---------------------------------------------------------------------------
create function public.push_targets(p_user_ids uuid[])
returns table (user_id uuid, fcm_token text)
language sql
stable
security definer
set search_path = ''
as $$
    select t.user_id, t.fcm_token from public.device_tokens t where t.user_id = any (p_user_ids);
$$;

-- FCM reported these tokens as no longer valid (app uninstalled, token rotated).
create function public.remove_device_tokens(p_tokens text[])
returns void
language sql
security definer
set search_path = ''
as $$
    delete from public.device_tokens where fcm_token = any (p_tokens);
$$;

revoke all on function public.register_device_token(text) from public, anon;
revoke all on function public.prepare_partner_push(text) from public, anon;
revoke all on function public.push_targets(uuid[]) from public, anon, authenticated;
revoke all on function public.remove_device_tokens(text[]) from public, anon, authenticated;
revoke all on function private.validate_time_zone() from public;
grant execute on function public.register_device_token(text) to authenticated;
grant execute on function public.prepare_partner_push(text) to authenticated;
grant execute on function public.push_targets(uuid[]) to service_role;
grant execute on function public.remove_device_tokens(text[]) to service_role;

-- ---------------------------------------------------------------------------
-- Reunion day (FR-NOT-4): at 08:00 on the reunion date in each partner's time zone, or
-- as soon as the date is set later that day, each partner is notified once.
--
-- The job calls send-push with a shared secret, kept in Vault (never in Git):
--   select vault.create_secret('<random secret>', 'push_internal_secret');
-- and set on the function: supabase secrets set PUSH_INTERNAL_SECRET=<same secret>
-- ---------------------------------------------------------------------------
create table private.reunion_day_notifications (
    couple_id uuid not null references public.couples (id) on delete cascade,
    user_id uuid not null references public.profiles (id) on delete cascade,
    meet_at timestamptz not null,
    sent_at timestamptz not null default now(),
    primary key (couple_id, user_id, meet_at)
);

-- Who should get the reunion-day notification now, recorded so each person gets it once.
create function private.claim_reunion_day_recipients()
returns uuid[]
language sql
security definer
set search_path = ''
as $$
    with due as (
        select r.couple_id, p.id as user_id, r.meet_at
        from public.reunions r
        join public.couples c on c.id = r.couple_id and c.status = 'active'
        join public.profiles p on p.id in (c.user_a, c.user_b)
        where (r.meet_at at time zone p.time_zone)::date = (now() at time zone p.time_zone)::date
          and (now() at time zone p.time_zone)::time >= time '08:00'
    ),
    claimed as (
        insert into private.reunion_day_notifications (couple_id, user_id, meet_at)
        select couple_id, user_id, meet_at from due
        on conflict do nothing
        returning user_id
    )
    select coalesce(array_agg(user_id), '{}') from claimed;
$$;

create function private.send_reunion_day_notifications()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    project_url text;
    internal_secret text;
    recipients uuid[];
begin
    select decrypted_secret into project_url from vault.decrypted_secrets where name = 'project_url';
    select decrypted_secret into internal_secret from vault.decrypted_secrets where name = 'push_internal_secret';
    if project_url is null or internal_secret is null then
        return;
    end if;

    recipients := private.claim_reunion_day_recipients();
    if cardinality(recipients) = 0 then
        return;
    end if;

    perform net.http_post(
        url := rtrim(project_url, '/') || '/functions/v1/send-push',
        headers := jsonb_build_object('Content-Type', 'application/json', 'x-internal-secret', internal_secret),
        body := jsonb_build_object('type', 'reunion_day', 'user_ids', to_jsonb(recipients))
    );
end;
$$;

revoke all on function private.claim_reunion_day_recipients() from public;
revoke all on function private.send_reunion_day_notifications() from public;

select cron.schedule(
    'twoverse-reunion-day-notifications',
    '*/15 * * * *',
    'select private.send_reunion_day_notifications()'
);
