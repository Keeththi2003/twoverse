-- Scheduled clean-up (FR-DEL-1, BR-6, BR-9 grace period, BR-10):
--   * expired memories are deleted, and their files queued for deletion
--   * couples whose 7-day disconnect grace period has passed are deleted with all data
--   * old codes and abandoned pending couples are removed
--   * queued files are deleted through the Storage API with pg_net
--
-- The Storage API call needs two Vault secrets, created once per project (never in Git):
--   select vault.create_secret('https://<project-ref>.supabase.co', 'project_url');
--   select vault.create_secret('<secret or service_role key>', 'storage_service_key');
-- Until they exist the queue simply waits.

create extension if not exists pg_net with schema extensions;
create extension if not exists pg_cron with schema pg_catalog;

-- Sends queued file deletions and clears the ones that succeeded.
create function private.process_storage_deletions(batch_size integer default 100)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    project_url text;
    service_key text;
    headers jsonb;
    item record;
begin
    select decrypted_secret into project_url from vault.decrypted_secrets where name = 'project_url';
    select decrypted_secret into service_key from vault.decrypted_secrets where name = 'storage_service_key';
    if project_url is null or service_key is null then
        return;
    end if;

    -- Done: deleted, or already gone (Storage answers 400/404 for a missing object).
    delete from private.storage_deletion_queue q
    using net._http_response r
    where r.id = q.request_id
      and (r.status_code between 200 and 299 or r.status_code in (400, 404));

    -- Failed or timed out: send again.
    update private.storage_deletion_queue
    set request_id = null, requested_at = null
    where request_id is not null and requested_at < now() - interval '10 minutes';

    -- New secret keys (sb_secret_…) go in the apikey header; legacy JWT keys also as Bearer.
    headers := jsonb_build_object('apikey', service_key);
    if service_key not like 'sb\_%' then
        headers := headers || jsonb_build_object('Authorization', 'Bearer ' || service_key);
    end if;

    for item in
        select q.id, q.bucket_id, q.object_path
        from private.storage_deletion_queue q
        where q.request_id is null
        order by q.id
        limit batch_size
        for update skip locked
    loop
        update private.storage_deletion_queue
        set request_id = net.http_delete(
                url := rtrim(project_url, '/') || '/storage/v1/object/' || item.bucket_id || '/' || item.object_path,
                headers := headers
            ),
            requested_at = now()
        where id = item.id;
    end loop;
end;
$$;

create function private.purge_expired_data()
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    delete from public.memories where expires_at <= now();

    delete from public.couples where status = 'ended' and purge_after <= now();

    delete from public.couple_codes where expires_at < now() - interval '7 days';

    delete from public.couples c
    where c.status = 'pending'
      and c.created_at < now() - interval '1 day'
      and not exists (
          select 1 from public.couple_codes cc
          where cc.couple_id = c.id and cc.used_at is null and cc.expires_at > now()
      );

    perform private.process_storage_deletions();
end;
$$;

revoke all on function private.process_storage_deletions(integer) from public;
revoke all on function private.purge_expired_data() from public;

select cron.schedule(
    'twoverse-purge-expired-data',
    '*/15 * * * *',
    'select private.purge_expired_data()'
);
