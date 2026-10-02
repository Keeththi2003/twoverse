begin;
select plan(5);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev'));

-- device_tokens
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.device_tokens (fcm_token, user_id) values ('token-a', %L)$$, tests.user_id('a@test.dev')),
    'a user registers their device token'
);
select throws_ok(
    format($$insert into public.device_tokens (fcm_token, user_id) values ('token-x', %L)$$, tests.user_id('b@test.dev')),
    '42501', null,
    'a user cannot register a token for someone else'
);
select tests.authenticate_as('b@test.dev');
select is_empty('select 1 from public.device_tokens', 'the partner cannot read the token');
delete from public.device_tokens;
select tests.authenticate_as('a@test.dev');
select results_eq('select fcm_token from public.device_tokens', $$values ('token-a'::text)$$, 'only the owner can delete it');
select lives_ok('delete from public.device_tokens', 'the owner deletes their token');

select * from finish();
rollback;
