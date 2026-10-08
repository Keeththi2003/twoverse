begin;
select plan(20);

-- Tables from SRS section 5
select has_table('public', t, format('table %s exists', t))
from unnest(array['profiles', 'couples', 'couple_codes', 'locations', 'reunions', 'memories', 'shooting_stars', 'meetups', 'device_tokens']) t;

select is(
    (select count(*)::int from pg_class c join pg_namespace n on n.oid = c.relnamespace
     where n.nspname = 'public' and c.relkind = 'r' and not c.relrowsecurity),
    0,
    'every public table has row level security enabled'
);

-- Profile created on sign-up from user metadata (FR-AUTH-5)
select tests.create_user('email@test.dev', '{"display_name": "Keeththi"}');
select tests.create_user('google@test.dev', '{"full_name": "Google Person"}');
select tests.create_user('plain.name@test.dev');

select is((select full_name from public.profiles where id = tests.user_id('email@test.dev')), 'Keeththi',
    'profile uses display_name metadata from older sign-up forms');
select is((select full_name from public.profiles where id = tests.user_id('google@test.dev')), 'Google Person',
    'profile falls back to full_name from Google');
select is((select full_name from public.profiles where id = tests.user_id('plain.name@test.dev')), 'plain.name',
    'profile falls back to the email local part');

-- One open couple per user, across both columns (BR-1, FR-PAIR-4)
select tests.create_couple(tests.user_id('email@test.dev'), tests.user_id('google@test.dev'));
select throws_ok(
    format('select tests.create_couple(%L, %L)', tests.user_id('plain.name@test.dev'), tests.user_id('email@test.dev')),
    'P0001', 'already_paired',
    'a user cannot be in two active couples'
);
select throws_ok(
    format($$insert into public.couples (user_a, user_b, status, connected_at) values (%L, %L, 'active', now())$$,
        tests.user_id('plain.name@test.dev'), tests.user_id('plain.name@test.dev')),
    '23514', null,
    'a couple needs two different users'
);

-- One location row per user, approximate positions rounded to about 1 km (FR-LOC-6, FR-LOC-7)
insert into public.locations (user_id, lat, lng, precision, sharing_enabled) values (tests.user_id('email@test.dev'), 6.927123, 79.861244, 'approximate', true);
select results_eq(
    format('select lat, lng from public.locations where user_id = %L', tests.user_id('email@test.dev')),
    $$values (6.93::double precision, 79.86::double precision)$$,
    'approximate locations are rounded to two decimals'
);
select throws_ok(
    format($$insert into public.locations (user_id, lat, lng) values (%L, 1, 1)$$, tests.user_id('email@test.dev')),
    '23505', null,
    'only one location row per user'
);
select throws_ok(
    format($$insert into public.locations (user_id, lat, lng, sharing_enabled) values (%L, 91, 1, true)$$, tests.user_id('google@test.dev')),
    '23514', null,
    'latitude must be valid'
);

-- Captions are limited to 500 characters (FR-MEM-3)
select throws_ok(
    format($$insert into public.memories (couple_id, sender_id, storage_path, caption) values (%L, %L, %L, repeat('x', 501))$$,
        (select id from public.couples where user_a = tests.user_id('email@test.dev')),
        tests.user_id('email@test.dev'),
        (select id from public.couples where user_a = tests.user_id('email@test.dev'))::text || '/a.jpg'),
    '23514', null,
    'captions longer than 500 characters are rejected'
);
select throws_ok(
    format($$insert into public.memories (couple_id, sender_id, storage_path) values (%L, %L, 'elsewhere/a.jpg')$$,
        (select id from public.couples where user_a = tests.user_id('email@test.dev')),
        tests.user_id('email@test.dev')),
    '23514', null,
    'memory files must be in the couple folder'
);

select * from finish();
rollback;
