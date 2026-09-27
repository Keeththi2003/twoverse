-- Pairing, disconnect, reconnect and account deletion (FR-PAIR, BR-9, BR-10, FR-SET-6).
-- Every function is security definer with an empty search_path, checks auth.uid(),
-- and runs in one transaction, so each rule is applied atomically (NFR-SEC-3).
--
-- Errors are raised with a stable message key the app maps to SRS section 7 texts:
--   not_authenticated, already_paired, invalid_code, code_expired, code_used,
--   own_code, not_paired, no_ended_couple

-- Serialises changes for one user (reentrant within a transaction).
create function private.lock_user(user_id uuid)
returns void
language sql
set search_path = ''
as $$
    select pg_advisory_xact_lock(hashtextextended(user_id::text, 0));
$$;

create function private.require_user()
returns uuid
language plpgsql
stable
set search_path = ''
as $$
declare
    me uuid := auth.uid();
begin
    if me is null then
        raise exception 'not_authenticated' using errcode = 'P0001';
    end if;
    return me;
end;
$$;

-- XXXX-XXXX from 32 unambiguous characters (no 0/O or 1/I), uniformly random.
create function private.random_couple_code()
returns text
language plpgsql
volatile
set search_path = ''
as $$
declare
    alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    bytes bytea := extensions.gen_random_bytes(8);
    result text := '';
begin
    for i in 0..7 loop
        if i = 4 then
            result := result || '-';
        end if;
        result := result || substr(alphabet, (get_byte(bytes, i) % 32) + 1, 1);
    end loop;
    return result;
end;
$$;

-- Deletes a user's ended couples still in their grace period, with all shared data.
create function private.purge_ended_couples_of(user_id uuid)
returns void
language sql
set search_path = ''
as $$
    delete from public.couples c
    where c.status = 'ended'
      and user_id in (c.user_a, c.user_b);
$$;

-- ---------------------------------------------------------------------------
-- create_couple_code(): a pending couple plus a one-time code valid for 24 hours
-- (FR-PAIR-1, BR-10). Creating a new code replaces the caller's unused ones.
-- ---------------------------------------------------------------------------
create function public.create_couple_code()
returns table (code text, expires_at timestamptz)
language plpgsql
security definer
set search_path = ''
as $$
#variable_conflict use_column
declare
    me uuid := private.require_user();
    pending_couple uuid;
    new_code text;
    new_expiry timestamptz := now() + interval '24 hours';
begin
    perform private.lock_user(me);

    if exists (select 1 from public.couples c where c.status = 'active' and me in (c.user_a, c.user_b)) then
        raise exception 'already_paired' using errcode = 'P0001';
    end if;

    select c.id into pending_couple
    from public.couples c
    where c.status = 'pending' and c.user_a = me;

    if pending_couple is null then
        insert into public.couples (user_a) values (me) returning id into pending_couple;
    end if;

    delete from public.couple_codes cc where cc.couple_id = pending_couple and cc.used_at is null;

    loop
        new_code := private.random_couple_code();
        begin
            insert into public.couple_codes (code, couple_id, created_by, expires_at)
            values (new_code, pending_couple, me, new_expiry);
            exit;
        exception when unique_violation then
            -- Code collision: try another one.
        end;
    end loop;

    return query select new_code, new_expiry;
end;
$$;

-- ---------------------------------------------------------------------------
-- join_couple(code): validate the code, link both users and consume the code in
-- one transaction (FR-PAIR-3 to FR-PAIR-5). Pairing ends any disconnect grace
-- period for both users early, deleting that old couple's data (SRS 12).
-- ---------------------------------------------------------------------------
create function public.join_couple(p_code text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    normalized text := upper(btrim(coalesce(p_code, '')));
    found_code public.couple_codes%rowtype;
    couple_status text;
begin
    if normalized !~ '^[A-Z0-9]{4}-[A-Z0-9]{4}$' then
        raise exception 'invalid_code' using errcode = 'P0001';
    end if;

    select cc.* into found_code
    from public.couple_codes cc
    where cc.code = normalized
    for update;

    if not found then
        raise exception 'invalid_code' using errcode = 'P0001';
    end if;

    -- Lock both users in a fixed order so two concurrent joins can't deadlock.
    perform private.lock_user(least(me, found_code.created_by));
    perform private.lock_user(greatest(me, found_code.created_by));

    select c.status into couple_status from public.couples c where c.id = found_code.couple_id for update;

    if found_code.created_by = me then
        raise exception 'own_code' using errcode = 'P0001';
    end if;
    if found_code.used_at is not null or couple_status is distinct from 'pending' then
        raise exception 'code_used' using errcode = 'P0001';
    end if;
    if found_code.expires_at <= now() then
        raise exception 'code_expired' using errcode = 'P0001';
    end if;
    if exists (select 1 from public.couples c where c.status = 'active' and me in (c.user_a, c.user_b)) then
        raise exception 'already_paired' using errcode = 'P0001';
    end if;

    perform private.purge_ended_couples_of(me);
    perform private.purge_ended_couples_of(found_code.created_by);
    delete from public.couples c where c.status = 'pending' and c.user_a = me;

    update public.couples
    set user_b = me, status = 'active', connected_at = now()
    where id = found_code.couple_id;

    update public.couple_codes
    set used_at = now(), used_by = me
    where code = normalized;

    return found_code.couple_id;
end;
$$;

-- ---------------------------------------------------------------------------
-- disconnect_couple(): ends the couple (FR-PAIR-7). Location sharing stops for
-- both immediately (BR-9); shared memories, reunion and birthday data become
-- unreadable and are deleted after a 7-day grace period unless both partners
-- confirm a reconnect (SRS 12).
-- ---------------------------------------------------------------------------
create function public.disconnect_couple()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    couple public.couples%rowtype;
begin
    perform private.lock_user(me);

    select c.* into couple
    from public.couples c
    where c.status = 'active' and me in (c.user_a, c.user_b)
    for update;

    if not found then
        raise exception 'not_paired' using errcode = 'P0001';
    end if;

    update public.couples
    set status = 'ended',
        ended_at = now(),
        ended_by = me,
        purge_after = now() + interval '7 days',
        reconnect_requested_by = null,
        reconnect_requested_at = null
    where id = couple.id;

    update public.locations
    set sharing_enabled = false
    where user_id in (couple.user_a, couple.user_b);
end;
$$;

-- ---------------------------------------------------------------------------
-- reconnect_couple(): reverses a disconnect during the grace period. Both
-- partners must confirm: the first call records the request and returns
-- 'requested'; the other partner's call restores the couple and returns 'active'.
-- ---------------------------------------------------------------------------
create function public.reconnect_couple()
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    couple public.couples%rowtype;
begin
    select c.* into couple
    from public.couples c
    where c.status = 'ended'
      and c.purge_after > now()
      and me in (c.user_a, c.user_b)
    order by c.ended_at desc
    limit 1
    for update;

    if not found then
        raise exception 'no_ended_couple' using errcode = 'P0001';
    end if;

    perform private.lock_user(least(couple.user_a, couple.user_b));
    perform private.lock_user(greatest(couple.user_a, couple.user_b));

    if couple.reconnect_requested_by is null or couple.reconnect_requested_by = me then
        update public.couples
        set reconnect_requested_by = me, reconnect_requested_at = now()
        where id = couple.id;
        return 'requested';
    end if;

    if exists (
        select 1 from public.couples c
        where c.status = 'active' and (c.user_a in (couple.user_a, couple.user_b) or c.user_b in (couple.user_a, couple.user_b))
    ) then
        raise exception 'already_paired' using errcode = 'P0001';
    end if;

    delete from public.couples c where c.status = 'pending' and c.user_a in (couple.user_a, couple.user_b);

    update public.couples
    set status = 'active',
        ended_at = null,
        ended_by = null,
        purge_after = null,
        reconnect_requested_by = null,
        reconnect_requested_at = null
    where id = couple.id;

    return 'active';
end;
$$;

-- ---------------------------------------------------------------------------
-- delete_account(): removes the caller's auth user (FR-SET-6). Cascades delete the
-- profile, location, device tokens, codes and every couple the user belongs to
-- with all shared data; photo files are queued for deletion by trigger.
-- ---------------------------------------------------------------------------
create function public.delete_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
begin
    perform private.lock_user(me);
    delete from auth.users where id = me;
end;
$$;

revoke all on function public.create_couple_code() from public, anon;
revoke all on function public.join_couple(text) from public, anon;
revoke all on function public.disconnect_couple() from public, anon;
revoke all on function public.reconnect_couple() from public, anon;
revoke all on function public.delete_account() from public, anon;
grant execute on function public.create_couple_code() to authenticated;
grant execute on function public.join_couple(text) to authenticated;
grant execute on function public.disconnect_couple() to authenticated;
grant execute on function public.reconnect_couple() to authenticated;
grant execute on function public.delete_account() to authenticated;

revoke all on function private.lock_user(uuid) from public;
revoke all on function private.require_user() from public;
revoke all on function private.random_couple_code() from public;
revoke all on function private.purge_ended_couples_of(uuid) from public;
