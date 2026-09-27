begin;
select plan(9);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_user('e@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;

select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.reunions (couple_id, meet_at, place, updated_by) values (%L, now() + interval '12 days', 'Kandy', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev')),
    'a member can set the reunion date'
);
select throws_ok(
    format($$insert into public.reunions (couple_id, meet_at, updated_by) values (%L, now(), %L)$$,
        (select cd from ids), tests.user_id('a@test.dev')),
    '42501', null,
    'a user cannot set another couple''s reunion'
);

select tests.authenticate_as('b@test.dev');
select results_eq('select place from public.reunions', $$values ('Kandy'::text)$$, 'the partner sees the same countdown (FR-CNT-2)');
select lives_ok(
    format($$update public.reunions set place = 'Colombo', updated_by = %L$$, tests.user_id('b@test.dev')),
    'the partner can edit the reunion'
);
select throws_ok(
    format($$update public.reunions set place = 'x', updated_by = %L$$, tests.user_id('a@test.dev')),
    '42501', null,
    'updated_by must be the editing user'
);

select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from public.reunions', 'another couple cannot read the reunion');
update public.reunions set place = 'hacked';
delete from public.reunions;
select tests.clear_authentication();
select results_eq('select place from public.reunions', $$values ('Colombo'::text)$$, 'another couple cannot change or clear it');

select tests.authenticate_as('e@test.dev');
select throws_ok(
    format($$insert into public.reunions (couple_id, meet_at, updated_by) values (%L, now(), %L)$$, gen_random_uuid(), tests.user_id('e@test.dev')),
    '42501', null,
    'an unpaired user cannot create a reunion'
);

select tests.authenticate_as('a@test.dev');
delete from public.reunions;
select tests.clear_authentication();
select is_empty('select 1 from public.reunions', 'a member can clear the date (FR-CNT-1)');

select * from finish();
rollback;
