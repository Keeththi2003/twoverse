begin;
select plan(4);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab;
grant select on ids to authenticated;

select tests.authenticate_as('a@test.dev');
insert into public.reunions (couple_id, meet_at, updated_by)
values ((select ab from ids), now() + interval '12 days', tests.user_id('a@test.dev'));
select tests.clear_authentication();
select ok(
    (select date_set_at > now() - interval '1 minute' from public.reunions),
    'setting a date records when it was set'
);

alter table public.reunions disable trigger reunions_date_set_at;
update public.reunions set date_set_at = now() - interval '3 days';
alter table public.reunions enable trigger reunions_date_set_at;

select tests.authenticate_as('b@test.dev');
update public.reunions set place = 'Kandy', note = 'Bring the camera', updated_by = tests.user_id('b@test.dev');
select tests.clear_authentication();
select ok(
    (select date_set_at < now() - interval '2 days' from public.reunions),
    'editing the place or note keeps when the date was set'
);

select tests.authenticate_as('b@test.dev');
update public.reunions set meet_at = meet_at + interval '1 day', updated_by = tests.user_id('b@test.dev');
select tests.clear_authentication();
select ok(
    (select date_set_at > now() - interval '1 minute' from public.reunions),
    'changing the date restarts the wait'
);

select tests.authenticate_as('a@test.dev');
update public.reunions set date_set_at = now() - interval '9 days', updated_by = tests.user_id('a@test.dev');
select tests.clear_authentication();
select ok(
    (select date_set_at > now() - interval '1 minute' from public.reunions),
    'clients cannot backdate date_set_at themselves'
);

select * from finish();
rollback;
