begin;
select plan(26);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
select tests.create_user('e@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
create temporary table ids_extra (ef uuid, gh uuid);

-- register_device_token
select set_config('role', 'authenticated', true);
select throws_ok($$select public.register_device_token('t')$$, 'P0001', 'not_authenticated', 'registering needs a signed-in user');

select tests.authenticate_as('b@test.dev');
select lives_ok($$select public.register_device_token('token-b')$$, 'a user registers their device token');
select lives_ok($$select public.register_device_token('token-b')$$, 'registering again is harmless');
select throws_ok($$select public.register_device_token('')$$, 'P0001', 'invalid_token', 'empty tokens are rejected');

select tests.authenticate_as('c@test.dev');
select public.register_device_token('token-c');
select tests.authenticate_as('d@test.dev');
select public.register_device_token('token-d');

select tests.authenticate_as('e@test.dev');
select public.register_device_token('shared-phone');
select tests.authenticate_as('a@test.dev');
select lives_ok($$select public.register_device_token('shared-phone')$$, 'a token moves to the account now using the phone');
select tests.clear_authentication();
select results_eq(
    $$select user_id from public.device_tokens where fcm_token = 'shared-phone'$$,
    format('values (%L::uuid)', tests.user_id('a@test.dev')),
    'the token belongs only to the new account'
);

-- prepare_partner_push: only the caller's partner (FR-NOT, NFR-SEC-3)
select set_config('role', 'authenticated', true);
select throws_ok($$select public.prepare_partner_push('wake_up')$$, 'P0001', 'not_authenticated', 'pushing needs a signed-in user');

select tests.authenticate_as('e@test.dev');
select throws_ok($$select public.prepare_partner_push('wake_up')$$, 'P0001', 'not_paired', 'an unpaired user cannot push');

select tests.authenticate_as('a@test.dev');
select throws_ok($$select public.prepare_partner_push('anything')$$, 'P0001', 'not_allowed', 'unknown kinds are refused');
select throws_ok($$select public.prepare_partner_push('reunion_day')$$, 'P0001', 'not_allowed', 'reunion-day pushes are server-only');
select results_eq(
    $$select fcm_token from public.prepare_partner_push('wake_up')$$,
    $$values ('token-b'::text)$$,
    'a wake-up ping goes only to the partner''s devices'
);
select throws_ok($$select public.prepare_partner_push('wake_up')$$, 'P0001', 'rate_limited', 'wake-up pings are limited to one a minute');

select tests.clear_authentication();
update private.push_rate_limits set last_sent_at = now() - interval '61 seconds' where kind = 'wake_up';
select tests.authenticate_as('a@test.dev');
select lives_ok($$select public.prepare_partner_push('wake_up')$$, 'a ping is allowed again after a minute');

select tests.authenticate_as('b@test.dev');
select results_eq(
    $$select fcm_token from public.prepare_partner_push('wake_up')$$,
    $$values ('shared-phone'::text)$$,
    'the partner''s limit is separate and reaches the caller''s partner'
);

select tests.authenticate_as('c@test.dev');
select results_eq(
    $$select fcm_token from public.prepare_partner_push('new_memory')$$,
    $$values ('token-d'::text)$$,
    'another couple only ever reaches their own partner'
);
select throws_ok($$select public.prepare_partner_push('new_memory')$$, 'P0001', 'rate_limited', 'new-memory pushes are rate-limited');

select tests.authenticate_as('b@test.dev');
select lives_ok($$select public.prepare_partner_push('partner_joined')$$, 'partner-joined can be sent right after pairing');
select throws_ok($$select public.prepare_partner_push('partner_joined')$$, 'P0001', 'rate_limited', 'but only once');
select tests.clear_authentication();
update public.couples set connected_at = now() - interval '1 hour' where id = (select cd from ids);
select tests.authenticate_as('c@test.dev');
select throws_ok($$select public.prepare_partner_push('partner_joined')$$, 'P0001', 'not_allowed', 'and not long after pairing');

-- Service-role helpers are not callable by users
select throws_ok(
    format($$select * from public.push_targets(array[%L::uuid])$$, tests.user_id('d@test.dev')),
    '42501', null,
    'users cannot read other people''s tokens'
);
select throws_ok($$select public.remove_device_tokens(array['token-d'])$$, '42501', null, 'users cannot remove other people''s tokens');
select set_config('role', 'anon', true);
select throws_ok($$select public.prepare_partner_push('wake_up')$$, '42501', null, 'signed-out clients cannot push');

-- Time zone
select tests.clear_authentication();
select throws_ok(
    format($$update public.profiles set time_zone = 'Mars/Olympus' where id = %L$$, tests.user_id('a@test.dev')),
    '22023', 'invalid_time_zone',
    'time zones must be real'
);

-- Reunion day (FR-NOT-4): 08:00 local on the day, once per person.
-- Time zones are picked so it is currently noon (after 08:00) or 03:00 (before) there.
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
select tests.create_user('f@test.dev');
select tests.create_user('g@test.dev');
select tests.create_user('h@test.dev');
insert into ids_extra select tests.create_couple(tests.user_id('e@test.dev'), tests.user_id('f@test.dev')), tests.create_couple(tests.user_id('g@test.dev'), tests.user_id('h@test.dev'));
update public.profiles set time_zone = pg_temp.zone_at_local_hour(12)
where id in (tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), tests.user_id('e@test.dev'), tests.user_id('f@test.dev'));
update public.profiles set time_zone = pg_temp.zone_at_local_hour(3)
where id in (tests.user_id('g@test.dev'), tests.user_id('h@test.dev'));

-- AB: today at 20:00 local; EF: tomorrow; GH: today, but it is 03:00 there
insert into public.reunions (couple_id, meet_at, updated_by)
select couple_id, (date_trunc('day', now() at time zone zone) + days + interval '20 hours') at time zone zone, updated_by
from (values
    ((select ab from ids), pg_temp.zone_at_local_hour(12), interval '0 days', tests.user_id('a@test.dev')),
    ((select ef from ids_extra), pg_temp.zone_at_local_hour(12), interval '1 day', tests.user_id('e@test.dev')),
    ((select gh from ids_extra), pg_temp.zone_at_local_hour(3), interval '0 days', tests.user_id('g@test.dev'))
) as r(couple_id, zone, days, updated_by);

create temporary table claims as
select private.claim_reunion_day_recipients() as first_run, private.claim_reunion_day_recipients() as second_run;
select is(
    (select array(select unnest(first_run) order by 1) from claims),
    array(select unnest(array[tests.user_id('a@test.dev'), tests.user_id('b@test.dev')]) order by 1),
    'both partners are notified on the reunion day after 08:00 local, and nobody else'
);
select is((select cardinality(second_run) from claims), 0, 'nobody is notified twice');
select lives_ok('select private.send_reunion_day_notifications()', 'the scheduled job runs without Vault secrets (it waits)');

select * from finish();
rollback;
