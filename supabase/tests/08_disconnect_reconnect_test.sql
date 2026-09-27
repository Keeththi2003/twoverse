begin;
select plan(24);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_user('e@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;
create temporary table codes (owner text primary key, code text);
grant all on codes to authenticated;

insert into public.locations (user_id, lat, lng, sharing_enabled)
values (tests.user_id('a@test.dev'), 6.9, 79.8, true), (tests.user_id('b@test.dev'), 7.5, 80.4, true);
insert into public.reunions (couple_id, meet_at, updated_by) values ((select ab from ids), now() + interval '12 days', tests.user_id('a@test.dev'));
insert into public.memories (couple_id, sender_id, storage_path) values ((select ab from ids), tests.user_id('a@test.dev'), (select ab from ids)::text || '/m.jpg');

-- disconnect_couple (FR-PAIR-7, BR-9, SRS 12)
select set_config('role', 'authenticated', true);
select throws_ok('select public.disconnect_couple()', 'P0001', 'not_authenticated', 'disconnect needs a signed-in user');
select tests.authenticate_as('e@test.dev');
select throws_ok('select public.disconnect_couple()', 'P0001', 'not_paired', 'an unpaired user cannot disconnect');

select tests.authenticate_as('a@test.dev');
select lives_ok('select public.disconnect_couple()', 'a partner disconnects');

select tests.clear_authentication();
select results_eq(
    format('select status, ended_by from public.couples where id = %L', (select ab from ids)),
    format($$values ('ended'::text, %L::uuid)$$, tests.user_id('a@test.dev')),
    'the couple is ended by the caller'
);
select ok(
    (select purge_after between now() + interval '7 days' - interval '1 minute' and now() + interval '7 days' + interval '1 minute'
     from public.couples where id = (select ab from ids)),
    'shared data is kept for a 7-day grace period'
);
select is((select count(*)::int from public.locations where sharing_enabled), 0, 'location sharing stops for both immediately (BR-9)');
select is((select count(*)::int from public.memories), 1, 'memories are kept during the grace period');

select tests.authenticate_as('b@test.dev');
select is_empty('select 1 from public.memories', 'memories are hidden after disconnecting');
select is_empty('select 1 from public.reunions', 'the reunion is hidden after disconnecting');
select is_empty(format('select 1 from public.locations where user_id = %L', tests.user_id('a@test.dev')), 'the partner''s location is hidden');
select is_empty(format('select 1 from public.profiles where id = %L', tests.user_id('a@test.dev')), 'the former partner''s profile is hidden');
select throws_ok('select public.disconnect_couple()', 'P0001', 'not_paired', 'disconnecting twice fails');

-- reconnect_couple: both must confirm within the grace period (SRS 12)
select tests.authenticate_as('c@test.dev');
select throws_ok('select public.reconnect_couple()', 'P0001', 'no_ended_couple', 'another couple cannot reconnect it');

select tests.authenticate_as('a@test.dev');
select is(public.reconnect_couple(), 'requested', 'the first confirmation records a request');
select is(public.reconnect_couple(), 'requested', 'confirming again alone does not reconnect');
select tests.clear_authentication();
select is((select status from public.couples where id = (select ab from ids)), 'ended', 'one confirmation is not enough');

select tests.authenticate_as('b@test.dev');
select is(public.reconnect_couple(), 'active', 'the partner''s confirmation restores the couple');
select is((select count(*)::int from public.memories), 1, 'shared memories are readable again');
select tests.clear_authentication();
select results_eq(
    format('select status, purge_after, reconnect_requested_by from public.couples where id = %L', (select ab from ids)),
    $$values ('active'::text, null::timestamptz, null::uuid)$$,
    'the restored couple has no pending clean-up'
);

-- After the grace period, reconnecting is impossible
select tests.authenticate_as('a@test.dev');
select public.disconnect_couple();
select tests.clear_authentication();
update public.couples set purge_after = now() - interval '1 minute' where id = (select ab from ids);
select tests.authenticate_as('b@test.dev');
select throws_ok('select public.reconnect_couple()', 'P0001', 'no_ended_couple', 'the grace period can''t be reversed once it has passed');

-- Pairing with someone new ends the grace period early and deletes the old data
select tests.clear_authentication();
update public.couples set purge_after = now() + interval '5 days' where id = (select ab from ids);
select tests.authenticate_as('a@test.dev');
insert into codes select 'a', code from public.create_couple_code();
select tests.authenticate_as('e@test.dev');
select lives_ok(format('select public.join_couple(%L)', (select code from codes where owner = 'a')), 'a disconnected user can pair with someone new');
select tests.clear_authentication();
select is_empty(format('select 1 from public.couples where id = %L', (select ab from ids)), 'the old couple is deleted');
select is_empty('select 1 from public.memories', 'with its memories');
select results_eq(
    'select object_path from private.storage_deletion_queue',
    format('values (%L)', (select ab from ids)::text || '/m.jpg'),
    'and its files are queued for deletion'
);

select * from finish();
rollback;
