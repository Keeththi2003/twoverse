begin;
select plan(9);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
create temporary table deleted as select tests.user_id('a@test.dev') as id;

insert into public.locations (user_id, lat, lng) values (tests.user_id('a@test.dev'), 1, 1), (tests.user_id('c@test.dev'), 2, 2);
insert into public.device_tokens (fcm_token, user_id) values ('token-a', tests.user_id('a@test.dev'));
insert into public.memories (couple_id, sender_id, storage_path) values
    ((select ab from ids), tests.user_id('b@test.dev'), (select ab from ids)::text || '/from-b.jpg'),
    ((select cd from ids), tests.user_id('c@test.dev'), (select cd from ids)::text || '/other.jpg');

select set_config('role', 'authenticated', true);
select throws_ok('select public.delete_account()', 'P0001', 'not_authenticated', 'delete_account needs a signed-in user');

select tests.authenticate_as('a@test.dev');
select lives_ok('select public.delete_account()', 'a user deletes their account (FR-SET-6)');

select tests.clear_authentication();
select is_empty('select 1 from auth.users u join deleted d on d.id = u.id', 'the auth user is removed');
select is_empty('select 1 from public.profiles p join deleted d on d.id = p.id', 'the profile is removed');
select is_empty('select 1 from public.locations l join deleted d on d.id = l.user_id', 'the location is removed');
select is_empty('select 1 from public.device_tokens t join deleted d on d.id = t.user_id', 'device tokens are removed');
select is_empty(format('select 1 from public.couples where id = %L', (select ab from ids)), 'the couple and its shared data are removed');
select results_eq(
    'select object_path from private.storage_deletion_queue',
    format('values (%L)', (select ab from ids)::text || '/from-b.jpg'),
    'the couple''s files are queued for deletion'
);
select results_eq(
    $$select (select count(*)::int from public.profiles), (select count(*)::int from public.memories), (select count(*)::int from public.locations)$$,
    $$values (3, 1, 1)$$,
    'other users and other couples are untouched'
);

select * from finish();
rollback;
