begin;
select plan(8);

select tests.create_user('a@test.dev', '{"display_name": "A"}');
select tests.create_user('b@test.dev', '{"display_name": "B"}');
select tests.create_user('c@test.dev', '{"display_name": "C"}');
select tests.create_user('d@test.dev', '{"display_name": "D"}');
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev'));
select tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev'));

select tests.authenticate_as('a@test.dev');

select results_eq(
    'select display_name from public.profiles order by display_name',
    $$values ('A'), ('B')$$,
    'a user reads their own and their partner''s profile only'
);
select is_empty(
    format('select 1 from public.profiles where id = %L', tests.user_id('c@test.dev')),
    'another couple''s profile is invisible'
);

update public.profiles set display_name = 'A2' where id = tests.user_id('a@test.dev');
update public.profiles set display_name = 'hacked' where id = tests.user_id('b@test.dev');
update public.profiles set display_name = 'hacked' where id = tests.user_id('c@test.dev');
select throws_ok(
    format($$insert into public.profiles (id, display_name) values (%L, 'x')$$, gen_random_uuid()),
    '42501', null,
    'profiles cannot be inserted by clients'
);
delete from public.profiles where id = tests.user_id('b@test.dev');

select tests.clear_authentication();
select is((select display_name from public.profiles where id = tests.user_id('a@test.dev')), 'A2', 'a user can edit their own profile');
select is((select display_name from public.profiles where id = tests.user_id('b@test.dev')), 'B', 'a user cannot edit their partner''s profile');
select is((select display_name from public.profiles where id = tests.user_id('c@test.dev')), 'C', 'a user cannot edit another couple''s profile');
select isnt_empty(format('select 1 from public.profiles where id = %L', tests.user_id('b@test.dev')), 'profiles cannot be deleted by clients');

select set_config('role', 'anon', true);
select throws_ok('select * from public.profiles', '42501', null, 'signed-out clients cannot read profiles');

select * from finish();
rollback;
