begin;
select plan(12);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev'));
select tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev'));

-- Settings can be saved before the first position (FR-LOC-2)
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.locations (user_id, sharing_enabled) values (%L, false)$$, tests.user_id('a@test.dev')),
    'sharing settings can be saved before the first position'
);
select results_eq(
    format('select sharing_enabled, precision from public.locations where user_id = %L', tests.user_id('a@test.dev')),
    $$values (false, 'approximate'::text)$$,
    'sharing is off and approximate by default'
);
select throws_ok(
    format($$update public.locations set sharing_enabled = true, lat = 1 where user_id = %L$$, tests.user_id('a@test.dev')),
    '23514', null,
    'latitude and longitude are stored together'
);

-- Sharing on with a position; approximate is rounded (FR-LOC-6)
update public.locations set sharing_enabled = true, lat = 6.927123, lng = 79.861244 where user_id = tests.user_id('a@test.dev');
select results_eq(
    format('select lat, lng from public.locations where user_id = %L', tests.user_id('a@test.dev')),
    $$values (6.93::double precision, 79.86::double precision)$$,
    'an approximate position is rounded to about 1 km'
);
select tests.authenticate_as('b@test.dev');
select isnt_empty(format('select 1 from public.locations where user_id = %L', tests.user_id('a@test.dev')), 'the partner sees a shared position');
select tests.authenticate_as('c@test.dev');
select is_empty(format('select 1 from public.locations where user_id = %L', tests.user_id('a@test.dev')), 'another couple does not');

-- updated_at only moves with the position (BR-8)
select tests.clear_authentication();
alter table public.locations disable trigger locations_position_time;
update public.locations set updated_at = now() - interval '1 hour' where user_id = tests.user_id('a@test.dev');
alter table public.locations enable trigger locations_position_time;
select tests.authenticate_as('a@test.dev');
update public.locations set precision = 'precise' where user_id = tests.user_id('a@test.dev');
select tests.clear_authentication();
select ok(
    (select updated_at < now() - interval '59 minutes' from public.locations where user_id = tests.user_id('a@test.dev')),
    'changing precision keeps the position''s time, so old data never looks live'
);
select tests.authenticate_as('a@test.dev');
update public.locations set precision = 'approximate' where user_id = tests.user_id('a@test.dev');
select tests.clear_authentication();
select ok(
    (select updated_at < now() - interval '59 minutes' from public.locations where user_id = tests.user_id('a@test.dev')),
    'rounding an unchanged position does not make it look new either'
);
select tests.authenticate_as('a@test.dev');
update public.locations set lat = 7.5, lng = 80.4 where user_id = tests.user_id('a@test.dev');
select tests.clear_authentication();
select results_eq(
    format('select lat, lng, updated_at > now() - interval ''1 minute'' from public.locations where user_id = %L', tests.user_id('a@test.dev')),
    $$values (7.5::double precision, 80.4::double precision, true)$$,
    'a new precise position is stored as sent and updates the time'
);

-- Turning sharing off removes the position (BR-3)
select tests.authenticate_as('a@test.dev');
update public.locations set sharing_enabled = false where user_id = tests.user_id('a@test.dev');
select tests.clear_authentication();
select results_eq(
    format('select lat, lng, accuracy_m from public.locations where user_id = %L', tests.user_id('a@test.dev')),
    $$values (null::double precision, null::double precision, null::real)$$,
    'turning sharing off clears the stored position'
);
select tests.authenticate_as('b@test.dev');
select is_empty(format('select 1 from public.locations where user_id = %L', tests.user_id('a@test.dev')), 'the partner no longer sees the row');

-- A position sent while sharing is off is not stored
select tests.authenticate_as('a@test.dev');
update public.locations set lat = 1, lng = 1 where user_id = tests.user_id('a@test.dev');
select tests.clear_authentication();
select is((select lat from public.locations where user_id = tests.user_id('a@test.dev')), null, 'positions are not stored while sharing is off');

select * from finish();
rollback;
