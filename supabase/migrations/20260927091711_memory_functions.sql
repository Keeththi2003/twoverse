-- Memory and birthday actions that change only specific columns, plus the queue
-- that removes photo files whenever their rows are deleted (FR-DEL-1, FR-DEL-3).
--
-- Errors: not_authenticated, memory_not_found, not_recipient, keep_not_allowed,
--         no_birthday_welcome

-- ---------------------------------------------------------------------------
-- Storage files to delete. Filled by triggers, emptied by the purge job, which
-- calls the Storage API (files can't be deleted with SQL).
-- ---------------------------------------------------------------------------
create table private.storage_deletion_queue (
    id bigint generated always as identity primary key,
    bucket_id text not null,
    object_path text not null,
    queued_at timestamptz not null default now(),
    request_id bigint,
    requested_at timestamptz
);

create function private.queue_memory_file_deletion()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into private.storage_deletion_queue (bucket_id, object_path)
    values ('memories', old.storage_path);
    return old;
end;
$$;

create trigger memories_queue_file_deletion
    after delete on public.memories
    for each row execute function private.queue_memory_file_deletion();

create function private.queue_birthday_photo_deletion()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if old.photo_path is not null and (tg_op = 'DELETE' or new.photo_path is distinct from old.photo_path) then
        insert into private.storage_deletion_queue (bucket_id, object_path)
        values ('memories', old.photo_path);
    end if;
    return coalesce(new, old);
end;
$$;

create trigger birthday_welcomes_queue_photo_deletion
    after update of photo_path or delete on public.birthday_welcomes
    for each row execute function private.queue_birthday_photo_deletion();

-- A memory the caller may act on: in their active couple and not expired (BR-6).
create function private.accessible_memory(memory_id uuid)
returns public.memories
language plpgsql
stable
set search_path = ''
as $$
declare
    memory public.memories%rowtype;
begin
    select m.* into memory
    from public.memories m
    where m.id = memory_id
      and m.couple_id = private.active_couple_id()
      and (m.expires_at is null or m.expires_at > now());
    if not found then
        raise exception 'memory_not_found' using errcode = 'P0001';
    end if;
    return memory;
end;
$$;

-- ---------------------------------------------------------------------------
-- mark_memory_viewed(id): clears the "new" dot for the recipient (FR-VLT-3).
-- ---------------------------------------------------------------------------
create function public.mark_memory_viewed(p_memory_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    memory public.memories%rowtype := private.accessible_memory(p_memory_id);
begin
    if memory.sender_id = me then
        return;
    end if;
    update public.memories set viewed_at = coalesce(viewed_at, now()) where id = memory.id;
end;
$$;

-- ---------------------------------------------------------------------------
-- hide_memory(id): the recipient removes a memory from their own vault only;
-- the sender controls its lifetime (SRS 12).
-- ---------------------------------------------------------------------------
create function public.hide_memory(p_memory_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    memory public.memories%rowtype := private.accessible_memory(p_memory_id);
begin
    if memory.sender_id = me then
        raise exception 'not_recipient' using errcode = 'P0001';
    end if;
    update public.memories set hidden_by_recipient_at = now() where id = memory.id;
end;
$$;

-- ---------------------------------------------------------------------------
-- keep_memory_forever(id): the recipient removes the expiry of a temporary memory
-- when the sender allowed it (FR-MEM-5, FR-DEL-2).
-- ---------------------------------------------------------------------------
create function public.keep_memory_forever(p_memory_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
    memory public.memories%rowtype := private.accessible_memory(p_memory_id);
begin
    if memory.sender_id = me then
        raise exception 'not_recipient' using errcode = 'P0001';
    end if;
    if not memory.allow_keep or memory.expires_at is null then
        raise exception 'keep_not_allowed' using errcode = 'P0001';
    end if;
    update public.memories set expires_at = null where id = memory.id;
end;
$$;

-- ---------------------------------------------------------------------------
-- mark_birthday_welcome_seen(): the recipient has opened it, so it shows once (FR-BDY-3).
-- ---------------------------------------------------------------------------
create function public.mark_birthday_welcome_seen()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
begin
    update public.birthday_welcomes
    set seen_at = coalesce(seen_at, now())
    where couple_id = private.active_couple_id() and for_user_id = me;
    if not found then
        raise exception 'no_birthday_welcome' using errcode = 'P0001';
    end if;
end;
$$;

revoke all on function public.mark_memory_viewed(uuid) from public, anon;
revoke all on function public.hide_memory(uuid) from public, anon;
revoke all on function public.keep_memory_forever(uuid) from public, anon;
revoke all on function public.mark_birthday_welcome_seen() from public, anon;
grant execute on function public.mark_memory_viewed(uuid) to authenticated;
grant execute on function public.hide_memory(uuid) to authenticated;
grant execute on function public.keep_memory_forever(uuid) to authenticated;
grant execute on function public.mark_birthday_welcome_seen() to authenticated;

revoke all on function private.accessible_memory(uuid) from public;
revoke all on function private.queue_memory_file_deletion() from public;
revoke all on function private.queue_birthday_photo_deletion() from public;
