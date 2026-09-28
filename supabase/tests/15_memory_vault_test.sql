begin;
select plan(6);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev'));
select tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev'));

-- Realtime (FR-VLT-1)
select ok(
    exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'memories'),
    'memories are published to realtime'
);

-- Lock Ours (FR-VLT-5)
select tests.authenticate_as('a@test.dev');
select is(
    (select lock_ours from public.profiles where id = tests.user_id('a@test.dev')),
    true,
    'Lock Ours is on by default'
);
update public.profiles set lock_ours = false where id = tests.user_id('a@test.dev');
select is(
    (select lock_ours from public.profiles where id = tests.user_id('a@test.dev')),
    false,
    'a user can turn off their own Lock Ours'
);

select tests.authenticate_as('b@test.dev');
update public.profiles set lock_ours = false where id = tests.user_id('a@test.dev');
update public.profiles set lock_ours = false where id = tests.user_id('c@test.dev');
select tests.clear_authentication();
select is(
    (select lock_ours from public.profiles where id = tests.user_id('c@test.dev')),
    true,
    'another couple''s user cannot change it'
);

select tests.authenticate_as('a@test.dev');
update public.profiles set lock_ours = true where id = tests.user_id('a@test.dev');
select tests.authenticate_as('b@test.dev');
update public.profiles set lock_ours = false where id = tests.user_id('a@test.dev');
select tests.clear_authentication();
select is(
    (select lock_ours from public.profiles where id = tests.user_id('a@test.dev')),
    true,
    'the partner cannot change it either'
);

select tests.authenticate_as('c@test.dev');
select is_empty(
    format('select lock_ours from public.profiles where id = %L', tests.user_id('a@test.dev')),
    'another couple cannot read it'
);

select * from finish();
rollback;
