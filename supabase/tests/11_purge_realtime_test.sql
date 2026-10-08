begin;
select plan(12);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_user('e@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;

insert into public.memories (couple_id, sender_id, storage_path, expires_at, created_at) values
    ((select ab from ids), tests.user_id('a@test.dev'), (select ab from ids)::text || '/expired.jpg', now() - interval '1 minute', now() - interval '1 day'),
    ((select ab from ids), tests.user_id('a@test.dev'), (select ab from ids)::text || '/later.jpg', now() + interval '1 day', now()),
    ((select cd from ids), tests.user_id('c@test.dev'), (select cd from ids)::text || '/ended.jpg', null, now());

-- CD disconnected more than 7 days ago; E has an abandoned pending couple
update public.couples set status = 'ended', ended_at = now() - interval '8 days', purge_after = now() - interval '1 day'
where id = (select cd from ids);
insert into public.couples (user_a, created_at) values (tests.user_id('e@test.dev'), now() - interval '2 days');
insert into public.couple_codes (code, couple_id, created_by, created_at, expires_at)
select 'OLDC-ODE1', id, user_a, now() - interval '9 days', now() - interval '8 days' from public.couples where user_a = tests.user_id('e@test.dev');

select private.purge_expired_data();

select results_eq(
    'select storage_path from public.memories',
    format('values (%L)', (select ab from ids)::text || '/later.jpg'),
    'expired memories and ended couples'' memories are deleted (FR-DEL-1)'
);
select is_empty(format('select 1 from public.couples where id = %L', (select cd from ids)), 'couples past the grace period are deleted');
select isnt_empty(format('select 1 from public.couples where id = %L', (select ab from ids)), 'active couples are kept');
select is_empty(format('select 1 from public.couples where user_a = %L', tests.user_id('e@test.dev')), 'abandoned pending couples are deleted');
select is_empty($$select 1 from public.couple_codes where code = 'OLDC-ODE1'$$, 'old codes are deleted');
select results_eq(
    'select object_path from private.storage_deletion_queue order by 1',
    format(
        'select unnest(array[%L, %L]) order by 1',
        (select ab from ids)::text || '/expired.jpg',
        (select cd from ids)::text || '/ended.jpg'
    ),
    'their files are queued'
);
select is(
    (select count(*)::int from private.storage_deletion_queue where request_id is not null),
    0,
    'without Vault secrets nothing is sent yet'
);

-- With the Vault secrets the queue is sent to the Storage API through pg_net
select vault.create_secret('http://storage.test', 'project_url');
select vault.create_secret('sb_secret_test', 'storage_service_key');
select private.process_storage_deletions();
select is(
    (select count(*)::int from private.storage_deletion_queue where request_id is not null),
    2,
    'queued files are sent to the Storage API'
);
select results_eq(
    $$select method::text, url, headers from net.http_request_queue order by url$$,
    format(
        $$select 'DELETE'::text, u, '{"apikey": "sb_secret_test"}'::jsonb from unnest(array[%L, %L]) as u order by u$$,
        'http://storage.test/storage/v1/object/memories/' || (select ab from ids)::text || '/expired.jpg',
        'http://storage.test/storage/v1/object/memories/' || (select cd from ids)::text || '/ended.jpg'
    ),
    'each file is deleted with a Storage API request'
);

-- Scheduling and Realtime
select results_eq(
    $$select schedule, command from cron.job where jobname = 'twoverse-purge-expired-data'$$,
    $$values ('*/15 * * * *'::text, 'select private.purge_expired_data()'::text)$$,
    'the purge job runs every 15 minutes'
);
select is(
    (select array_agg(tablename::text order by tablename) from pg_publication_tables
     where pubname = 'supabase_realtime' and schemaname = 'public'),
    array['couples', 'locations', 'meetups', 'memories', 'reunions'],
    'realtime is enabled for couples, locations, meetups, memories and reunions'
);
select tests.authenticate_as('a@test.dev');
select throws_ok('select private.purge_expired_data()', '42501', null, 'clients cannot run the purge job');

select * from finish();
rollback;
