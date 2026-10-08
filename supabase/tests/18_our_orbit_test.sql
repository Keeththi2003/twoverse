begin;
select plan(37);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_user('e@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;

-- Time zones where it is now noon (after 08:00) or 03:00 (before), as in 13_push_test.
create function pg_temp.zone_at_local_hour(local_hour int) returns text language sql as $$
    select case
        when o = 0 then 'Etc/GMT'
        when o > 0 then 'Etc/GMT-' || o
        else 'Etc/GMT+' || -o
    end
    from (
        select case when x > 14 then x - 24 when x < -12 then x + 24 else x end as o
        from (select local_hour - extract(hour from now() at time zone 'UTC')::int as x) raw
    ) normalized;
$$;
update public.profiles set time_zone = pg_temp.zone_at_local_hour(12)
where id in (tests.user_id('a@test.dev'), tests.user_id('b@test.dev'));
update public.profiles set time_zone = pg_temp.zone_at_local_hour(3)
where id in (tests.user_id('c@test.dev'), tests.user_id('d@test.dev'));
create temporary table local_today as select (now() at time zone pg_temp.zone_at_local_hour(12))::date as noon_zone;
grant select on local_today to authenticated;

-- set_together_since (FR-ORB-1)
select set_config('role', 'authenticated', true);
select throws_ok($$select public.set_together_since('2024-02-14')$$, 'P0001', 'not_authenticated', 'setting the date needs a signed-in user');

select tests.authenticate_as('e@test.dev');
select throws_ok($$select public.set_together_since('2024-02-14')$$, 'P0001', 'not_paired', 'an unpaired user has no date to set');

select tests.authenticate_as('a@test.dev');
select throws_ok($$select public.set_together_since(null)$$, 'P0001', 'invalid_date', 'the date is required');
select throws_ok(
    format('select public.set_together_since(%L)', (select noon_zone + 1 from local_today)),
    'P0001', 'date_in_future',
    'the date cannot be in the future in the caller''s time zone'
);
select lives_ok(
    format('select public.set_together_since(%L)', (select noon_zone from local_today)),
    'today is allowed'
);
select lives_ok($$select public.set_together_since('2024-02-14')$$, 'a partner sets the date');
select results_eq(
    format('select together_since, together_since_set_by from public.couples where id = %L', (select ab from ids)),
    format($$values ('2024-02-14'::date, %L::uuid)$$, tests.user_id('a@test.dev')),
    'the date and who set it are recorded'
);

select tests.authenticate_as('b@test.dev');
select lives_ok($$select public.set_together_since('2024-02-10')$$, 'the other partner can change it');
select results_eq(
    format('select together_since, together_since_set_by, together_since_set_at is not null from public.couples where id = %L', (select ab from ids)),
    format($$values ('2024-02-10'::date, %L::uuid, true)$$, tests.user_id('b@test.dev')),
    'the change records the new author and time'
);
update public.couples set together_since = '2000-01-01';
select tests.authenticate_as('c@test.dev');
select is_empty(
    format('select 1 from public.couples where id = %L', (select ab from ids)),
    'another couple cannot read the date'
);
select tests.clear_authentication();
select is(
    (select together_since from public.couples where id = (select ab from ids)),
    '2024-02-10'::date,
    'the couples table cannot be changed directly, only through the function'
);
select is((select together_since from public.couples where id = (select cd from ids)), null::date, 'another couple is untouched');

-- meetups (FR-ORB-3, FR-ORB-12)
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.meetups (couple_id, start_date, end_date, place, note, created_by)
             values (%L, '2026-09-10', '2026-09-14', 'Kandy', 'Long weekend', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev')),
    'a partner adds a meetup'
);
select throws_ok(
    format($$insert into public.meetups (couple_id, start_date, created_by) values (%L, '2026-09-01', %L)$$,
        (select ab from ids), tests.user_id('b@test.dev')),
    '42501', null,
    'a meetup cannot be added in the partner''s name'
);
select throws_ok(
    format($$insert into public.meetups (couple_id, start_date, created_by) values (%L, '2026-09-01', %L)$$,
        (select cd from ids), tests.user_id('a@test.dev')),
    '42501', null,
    'a meetup cannot be added to another couple'
);
select throws_ok(
    format($$insert into public.meetups (couple_id, start_date, end_date, created_by) values (%L, '2026-09-10', '2026-09-09', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev')),
    '23514', null,
    'the end date cannot be before the start date'
);
select throws_ok(
    format($$insert into public.meetups (couple_id, start_date, note, created_by) values (%L, '2026-09-10', repeat('x', 301), %L)$$,
        (select ab from ids), tests.user_id('a@test.dev')),
    '23514', null,
    'notes are limited to 300 characters'
);
select throws_ok(
    format($$insert into public.meetups (couple_id, start_date, place, created_by) values (%L, '2026-09-10', '  ', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev')),
    '23514', null,
    'an empty place is stored as null, never as blank text'
);
select lives_ok(
    format($$insert into public.meetups (couple_id, start_date, created_by) values (%L, '2026-09-20', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev')),
    'a single-day meetup has no end date'
);

select tests.authenticate_as('b@test.dev');
select results_eq(
    'select place from public.meetups order by start_date',
    $$values ('Kandy'::text), (null)$$,
    'the partner reads the couple''s meetups'
);
select lives_ok($$update public.meetups set note = 'Best weekend' where place = 'Kandy'$$, 'the partner edits a meetup');
select throws_ok(
    format($$update public.meetups set couple_id = %L$$, (select cd from ids)),
    '42501', null,
    'a meetup cannot be moved to another couple'
);
select throws_ok(
    format($$update public.meetups set created_by = %L$$, tests.user_id('b@test.dev')),
    '42501', null,
    'the author cannot be changed'
);
select lives_ok($$delete from public.meetups where place is null$$, 'the partner deletes a meetup');

select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from public.meetups', 'another couple cannot read the meetups');
update public.meetups set note = 'hacked';
delete from public.meetups;
select set_config('role', 'anon', true);
select throws_ok('select 1 from public.meetups', '42501', null, 'signed-out clients cannot read meetups');

select tests.clear_authentication();
select results_eq(
    'select place, note from public.meetups',
    $$values ('Kandy'::text, 'Best weekend'::text)$$,
    'another couple cannot change or delete them'
);

-- A passed reunion is recorded as a meetup only once (FR-ORB-10)
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.meetups (couple_id, start_date, created_by, from_reunion_at) values (%L, '2026-10-10', %L, '2026-10-10T08:00:00Z')$$,
        (select ab from ids), tests.user_id('a@test.dev')),
    'a reunion is recorded as a meetup'
);
select tests.authenticate_as('b@test.dev');
select throws_ok(
    format($$insert into public.meetups (couple_id, start_date, created_by, from_reunion_at) values (%L, '2026-10-10', %L, '2026-10-10T08:00:00Z')$$,
        (select ab from ids), tests.user_id('b@test.dev')),
    '23505', null,
    'the partner cannot record the same reunion again'
);

select tests.clear_authentication();
select ok(
    exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'meetups'),
    'meetups are published for Realtime'
);

-- Anniversary and milestone pushes (FR-NOT-6)
-- AB: their 2nd anniversary today. CD: day 100 today, but it is 03:00 there.
update public.couples set together_since = (select noon_zone - interval '2 years' from local_today)::date where id = (select ab from ids);
update public.couples set together_since = (now() at time zone pg_temp.zone_at_local_hour(3))::date - 99 where id = (select cd from ids);
select results_eq(
    'select kind, count, user_ids from private.claim_orbit_recipients()',
    format($$values ('anniversary'::text, 2, array[%L::uuid, %L::uuid])$$,
        least(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
        greatest(tests.user_id('a@test.dev'), tests.user_id('b@test.dev'))),
    'both partners get the anniversary after 08:00 local, and nobody before 08:00'
);
select is_empty('select * from private.claim_orbit_recipients()', 'nobody is notified twice on the same day');

delete from private.orbit_notifications;
update public.couples set together_since = (select noon_zone - 999 from local_today) where id = (select ab from ids);
select results_eq(
    'select kind, count from private.claim_orbit_recipients()',
    $$values ('orbit_milestone'::text, 1000)$$,
    'day 1000 is a milestone (the start date is day 1)'
);

delete from private.orbit_notifications;
update public.couples set together_since = (select noon_zone - 100 from local_today) where id = (select ab from ids);
select is_empty('select * from private.claim_orbit_recipients()', 'an ordinary day sends nothing');

select is(
    ('2024-02-29'::date + make_interval(years => 3))::date,
    '2027-02-28'::date,
    'a 29 February anniversary falls on 28 February in years without one'
);

select tests.authenticate_as('a@test.dev');
select throws_ok('select * from private.claim_orbit_recipients()', '42501', null, 'users cannot claim notifications');
select tests.clear_authentication();
select lives_ok('select private.send_orbit_notifications()', 'the scheduled job runs without Vault secrets (it waits)');

select * from finish();
rollback;
