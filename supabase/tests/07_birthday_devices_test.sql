begin;
select plan(15);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;

-- birthday_welcomes (FR-BDY-1 to FR-BDY-3)
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.birthday_welcomes (couple_id, for_user_id, created_by, message, photo_path) values (%L, %L, %L, 'Happy birthday', %L)$$,
        (select ab from ids), tests.user_id('b@test.dev'), tests.user_id('a@test.dev'), (select ab from ids)::text || '/bday.jpg'),
    'a user writes a birthday welcome for their partner'
);
select throws_ok(
    format($$insert into public.birthday_welcomes (couple_id, for_user_id, created_by, message) values (%L, %L, %L, 'x')$$,
        (select cd from ids), tests.user_id('c@test.dev'), tests.user_id('a@test.dev')),
    '42501', null,
    'a user cannot write one for another couple'
);
select throws_ok('select public.mark_birthday_welcome_seen()', 'P0001', 'no_birthday_welcome', 'the author has no welcome to mark seen');

select tests.authenticate_as('b@test.dev');
select results_eq('select message from public.birthday_welcomes', $$values ('Happy birthday'::text)$$, 'the recipient reads it');
update public.birthday_welcomes set message = 'changed';
select lives_ok('select public.mark_birthday_welcome_seen()', 'the recipient marks it seen');

select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from public.birthday_welcomes', 'another couple cannot read it');
update public.birthday_welcomes set message = 'hacked';
delete from public.birthday_welcomes;

select tests.clear_authentication();
select results_eq(
    'select message, seen_at is not null from public.birthday_welcomes',
    $$values ('Happy birthday'::text, true)$$,
    'only the author edits it; the recipient''s seen mark is stored'
);

select tests.authenticate_as('a@test.dev');
select lives_ok($$update public.birthday_welcomes set photo_path = null$$, 'the author edits it');
select lives_ok('delete from public.birthday_welcomes', 'the author deletes it');
select tests.clear_authentication();
select results_eq(
    'select object_path from private.storage_deletion_queue',
    format('values (%L)', (select ab from ids)::text || '/bday.jpg'),
    'a replaced birthday photo is queued for deletion'
);

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
