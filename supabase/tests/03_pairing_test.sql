begin;
select plan(26);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('e@test.dev');
select tests.create_user('f@test.dev');
select tests.create_user('g@test.dev');
select tests.create_user('h@test.dev');

create temporary table codes (owner text primary key, code text, expires_at timestamptz);
grant all on codes to authenticated;

-- Signed out: no user id
select set_config('role', 'authenticated', true);
select throws_ok('select public.create_couple_code()', 'P0001', 'not_authenticated', 'create_couple_code needs a signed-in user');
select throws_ok($$select public.join_couple('ABCD-EFGH')$$, 'P0001', 'not_authenticated', 'join_couple needs a signed-in user');

-- create_couple_code (FR-PAIR-1, BR-10)
select tests.authenticate_as('a@test.dev');
insert into codes select 'a-first', code, expires_at from public.create_couple_code();
insert into codes select 'a', code, expires_at from public.create_couple_code();

select matches((select code from codes where owner = 'a'), '^[A-Z0-9]{4}-[A-Z0-9]{4}$', 'code has the XXXX-XXXX format');
select ok(
    (select expires_at between now() + interval '23 hours 59 minutes' and now() + interval '24 hours 1 minute' from codes where owner = 'a'),
    'code is valid for 24 hours'
);
select results_eq(
    $$select status, user_b from public.couples$$,
    $$values ('pending'::text, null::uuid)$$,
    'creating a code makes one pending couple, reused for new codes'
);
select results_eq(
    'select code from public.couple_codes',
    'select code from codes where owner = ''a''',
    'a new code replaces the unused previous one'
);
select throws_ok(
    format('select public.join_couple(%L)', (select code from codes where owner = 'a')),
    'P0001', 'own_code',
    'a user cannot join their own code'
);

-- join_couple (FR-PAIR-3 to FR-PAIR-5)
select tests.authenticate_as('b@test.dev');
select is_empty('select 1 from public.couple_codes', 'codes are only visible to their creator');
select throws_ok($$select public.join_couple('not a code')$$, 'P0001', 'invalid_code', 'malformed codes are invalid');
select throws_ok($$select public.join_couple('ZZZZ-ZZZZ')$$, 'P0001', 'invalid_code', 'unknown codes are invalid');
select throws_ok(
    format('select public.join_couple(%L)', (select code from codes where owner = 'a-first')),
    'P0001', 'invalid_code',
    'a replaced code no longer works'
);
select lives_ok(
    format('select public.join_couple(%L)', '  ' || lower((select code from codes where owner = 'a')) || ' '),
    'joining is case- and space-insensitive'
);

select tests.clear_authentication();
select results_eq(
    format('select user_a, user_b, status from public.couples where user_a = %L', tests.user_id('a@test.dev')),
    format($$values (%L::uuid, %L::uuid, 'active'::text)$$, tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    'joining links both users in an active couple'
);
select ok(
    (select connected_at is not null from public.couples where user_a = tests.user_id('a@test.dev')),
    'the couple records when it connected'
);
select results_eq(
    'select used_by from public.couple_codes',
    format('values (%L::uuid)', tests.user_id('b@test.dev')),
    'the code is consumed by the joiner'
);

-- Codes work only once (BR-10)
select tests.authenticate_as('c@test.dev');
select throws_ok(
    format('select public.join_couple(%L)', (select code from codes where owner = 'a')),
    'P0001', 'code_used',
    'a used code cannot be reused'
);

-- Expired codes fail (BR-10)
select tests.authenticate_as('e@test.dev');
insert into codes select 'e', code, expires_at from public.create_couple_code();
select tests.clear_authentication();
update public.couple_codes set created_at = now() - interval '25 hours', expires_at = now() - interval '1 minute'
where created_by = tests.user_id('e@test.dev');
select tests.authenticate_as('f@test.dev');
select throws_ok(
    format('select public.join_couple(%L)', (select code from codes where owner = 'e')),
    'P0001', 'code_expired',
    'an expired code cannot be used'
);

-- One couple per user (FR-PAIR-4)
select tests.authenticate_as('a@test.dev');
select throws_ok('select public.create_couple_code()', 'P0001', 'already_paired', 'a paired user cannot create a code');

select tests.authenticate_as('g@test.dev');
insert into codes select 'g', code, expires_at from public.create_couple_code();

select tests.authenticate_as('a@test.dev');
select throws_ok(
    format('select public.join_couple(%L)', (select code from codes where owner = 'g')),
    'P0001', 'already_paired',
    'a paired user cannot join a second couple'
);
select tests.authenticate_as('b@test.dev');
select throws_ok(
    format('select public.join_couple(%L)', (select code from codes where owner = 'g')),
    'P0001', 'already_paired',
    'the partner who joined cannot join a second couple either'
);

-- A joiner's own pending code is removed when they join someone else
select tests.authenticate_as('h@test.dev');
insert into codes select 'h', code, expires_at from public.create_couple_code();
select lives_ok(format('select public.join_couple(%L)', (select code from codes where owner = 'g')), 'an unpaired user can join');
select tests.clear_authentication();
select is_empty(
    format($$select 1 from public.couples where user_a = %L$$, tests.user_id('h@test.dev')),
    'the joiner''s own pending couple is removed'
);
select is_empty(
    format('select 1 from public.couple_codes where code = %L', (select code from codes where owner = 'h')),
    'and so is their unused code'
);

-- Couples and codes are changed only through the functions
select tests.authenticate_as('c@test.dev');
select throws_ok(
    format($$insert into public.couples (user_a) values (%L)$$, tests.user_id('c@test.dev')),
    '42501', null,
    'clients cannot create couples directly'
);
update public.couples set status = 'ended';
select tests.clear_authentication();
select is(
    (select count(*)::int from public.couples where status = 'active'),
    2,
    'clients cannot change couples directly'
);

select set_config('role', 'anon', true);
select throws_ok($$select public.join_couple('ABCD-EFGH')$$, '42501', null, 'signed-out clients cannot call join_couple');

select * from finish();
rollback;
