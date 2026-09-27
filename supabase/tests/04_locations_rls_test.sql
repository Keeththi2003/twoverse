begin;
select plan(10);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev'));
select tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev'));

select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.locations (user_id, lat, lng, precision, sharing_enabled) values (%L, 6.9, 79.8, 'precise', true)$$,
        tests.user_id('a@test.dev')),
    'a user can store their own location'
);
select throws_ok(
    format($$insert into public.locations (user_id, lat, lng) values (%L, 1, 1)$$, tests.user_id('b@test.dev')),
    '42501', null,
    'a user cannot write their partner''s location'
);

select tests.authenticate_as('b@test.dev');
insert into public.locations (user_id, lat, lng, sharing_enabled) values (tests.user_id('b@test.dev'), 7.5, 80.4, false);
select results_eq(
    'select user_id from public.locations order by user_id = auth.uid()',
    format('values (%L::uuid), (%L::uuid)', tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    'the partner''s shared location is readable'
);

select tests.authenticate_as('a@test.dev');
select results_eq(
    'select user_id from public.locations',
    format('values (%L::uuid)', tests.user_id('a@test.dev')),
    'the partner''s location is hidden while they don''t share it (FR-LOC-2)'
);
update public.locations set lat = 0 where user_id = tests.user_id('b@test.dev');
delete from public.locations where user_id = tests.user_id('b@test.dev');

select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from public.locations', 'another couple cannot read shared locations');
update public.locations set lat = 0;
delete from public.locations;

select tests.clear_authentication();
select results_eq(
    format('select sharing_enabled, lat from public.locations where user_id = %L', tests.user_id('b@test.dev')),
    $$values (false, null::double precision)$$,
    'a user cannot change their partner''s location (not stored while unshared)'
);
select is((select count(*)::int from public.locations), 2, 'no one can delete someone else''s location');

select tests.authenticate_as('b@test.dev');
update public.locations set sharing_enabled = true where user_id = tests.user_id('b@test.dev');
select tests.authenticate_as('a@test.dev');
select isnt_empty(
    format('select 1 from public.locations where user_id = %L', tests.user_id('b@test.dev')),
    'turning sharing on makes the location readable'
);
select lives_ok(
    format('delete from public.locations where user_id = %L', tests.user_id('a@test.dev')),
    'a user can delete their own location'
);

select set_config('role', 'anon', true);
select throws_ok('select * from public.locations', '42501', null, 'signed-out clients cannot read locations');

select * from finish();
rollback;
