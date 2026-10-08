begin;
select plan(30);

select tests.create_user('a@test.dev', '{"full_name": "Keeththi Lan"}');
select tests.create_user('b@test.dev', '{"full_name": "Ammu Perera", "short_name": "Ammu"}');
select tests.create_user('c@test.dev', '{"name": "Chris Doe"}');
select tests.create_user('d@test.dev', '{"display_name": "Dee"}');
select tests.create_user('e@test.dev', format('{"full_name": "%s Smith"}', repeat('x', 40))::jsonb);
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;

-- Names on sign-up (FR-PRO-1)
select results_eq(
    $$select full_name, short_name from public.profiles order by full_name$$,
    format($$values ('Ammu Perera'::text, 'Ammu'::text), ('Chris Doe', 'Chris'), ('Dee', 'Dee'), ('Keeththi Lan', 'Keeththi'), (%L, %L)$$,
        repeat('x', 40) || ' Smith', repeat('x', 30)),
    'the short name is the sign-up form''s, or the first word of the full name (at most 30 characters)'
);
select throws_ok(
    format($$update public.profiles set short_name = '  ' where id = %L$$, tests.user_id('a@test.dev')),
    '23514', null,
    'the short name cannot be blank'
);

-- Editing your own profile (FR-PRO-4)
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$update public.profiles set pronouns = 'she', phone = '+94771234567', short_name = 'Kee' where id = %L$$, tests.user_id('a@test.dev')),
    'a user edits their own pronouns, phone and short name'
);
select throws_ok(
    format($$update public.profiles set phone = '0771234567' where id = %L$$, tests.user_id('a@test.dev')),
    '23514', null,
    'phone numbers are stored with their country code'
);
select results_eq(
    'select short_name, pronouns::text, phone, share_email, share_phone from public.get_my_profile()',
    $$values ('Kee'::text, 'she'::text, '+94771234567'::text, false, false)$$,
    'a user reads their own phone and sharing choices'
);
update public.profiles set pronouns = 'he', short_name = 'hacked' where id = tests.user_id('b@test.dev');

select tests.authenticate_as('b@test.dev');
update public.profiles set phone = '+94770000000', short_name = 'hacked' where id = tests.user_id('a@test.dev');
select tests.clear_authentication();
select results_eq(
    format('select short_name, phone from public.profiles where id in (%L, %L) order by short_name', tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    $$values ('Ammu'::text, null::text), ('Kee', '+94771234567')$$,
    'users cannot edit their partner''s profile'
);

-- The partner never reads unshared phone or email (FR-PRO-6)
select tests.authenticate_as('b@test.dev');
select throws_ok('select phone from public.profiles', '42501', null, 'clients cannot select phone numbers directly');
select throws_ok('select share_email from public.profiles', '42501', null, 'clients cannot select sharing choices directly');
select results_eq(
    'select full_name, short_name, pronouns::text, email, phone, nickname from public.get_partner_profile()',
    $$values ('Keeththi Lan'::text, 'Kee'::text, 'she'::text, null::text, null::text, null::text)$$,
    'the partner sees names and pronouns, but not unshared email or phone'
);

select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$update public.profiles set share_email = true, share_phone = true where id = %L$$, tests.user_id('a@test.dev')),
    'a user chooses to share their email and phone'
);
select tests.authenticate_as('b@test.dev');
select results_eq(
    'select email, phone from public.get_partner_profile()',
    $$values ('a@test.dev'::text, '+94771234567'::text)$$,
    'shared email and phone reach the partner'
);

select tests.authenticate_as('c@test.dev');
select results_eq(
    'select short_name from public.get_partner_profile()',
    $$values ('Dee'::text)$$,
    'another couple only ever gets their own partner'
);
select is_empty(
    format('select 1 from public.profiles where id = %L', tests.user_id('a@test.dev')),
    'another couple cannot read the profile'
);

select tests.authenticate_as('e@test.dev');
select is_empty('select * from public.get_partner_profile()', 'an unpaired user has no partner profile');
select set_config('role', 'authenticated', true);
select set_config('request.jwt.claims', null, true);
select throws_ok('select * from public.get_partner_profile()', 'P0001', 'not_authenticated', 'the partner profile needs a signed-in user');
select set_config('role', 'anon', true);
select throws_ok('select * from public.get_partner_profile()', '42501', null, 'signed-out clients cannot call it');

-- Private nicknames (FR-PRO-1, FR-PRO-5)
select tests.authenticate_as('a@test.dev');
select lives_ok($$select public.set_partner_nickname('  Chellam  ')$$, 'a user sets a nickname for their partner');
select results_eq('select nickname from public.get_partner_profile()', $$values ('Chellam'::text)$$, 'they see it on their partner');
select throws_ok($$select public.set_partner_nickname(repeat('x', 31))$$, 'P0001', 'invalid_nickname', 'nicknames are at most 30 characters');
select throws_ok(
    format($$insert into public.partner_nicknames (user_id, partner_id, nickname) values (%L, %L, 'x')$$, tests.user_id('a@test.dev'), tests.user_id('c@test.dev')),
    '42501', null,
    'a nickname can only be for the current partner'
);

select tests.authenticate_as('b@test.dev');
select is_empty('select 1 from public.partner_nicknames', 'the partner cannot read the nickname set for them');
select results_eq('select nickname from public.get_partner_profile()', $$values (null::text)$$, 'and it never appears in their partner profile');
update public.partner_nicknames set nickname = 'hacked';
delete from public.partner_nicknames;
select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from public.partner_nicknames', 'another couple cannot read it');

select tests.clear_authentication();
select results_eq('select nickname from public.partner_nicknames', $$values ('Chellam'::text)$$, 'only the user who set it can change it');

select tests.authenticate_as('e@test.dev');
select throws_ok($$select public.set_partner_nickname('x')$$, 'P0001', 'not_paired', 'an unpaired user has nobody to nickname');

-- Pushes name the partner the way the recipient knows them (FR-NOT-7)
select tests.authenticate_as('b@test.dev');
select lives_ok($$select public.set_partner_nickname('Kanna')$$, 'the partner sets their own nickname');
select public.register_device_token('token-b');
select tests.authenticate_as('a@test.dev');
select public.register_device_token('token-a');
select results_eq(
    $$select fcm_token, partner_name, partner_pronouns from public.prepare_partner_push('new_memory')$$,
    $$values ('token-b'::text, 'Kanna'::text, 'she'::text)$$,
    'a push to the partner names the sender with the partner''s nickname for them'
);

select tests.clear_authentication();
select results_eq(
    format('select fcm_token, partner_name, partner_pronouns from public.push_targets(array[%L::uuid])', tests.user_id('a@test.dev')),
    $$values ('token-a'::text, 'Chellam'::text, null::text)$$,
    'server pushes name the recipient''s partner the same way'
);
delete from public.partner_nicknames where user_id = tests.user_id('a@test.dev');
select results_eq(
    format('select partner_name from public.push_targets(array[%L::uuid])', tests.user_id('a@test.dev')),
    $$values ('Ammu'::text)$$,
    'without a nickname, the partner''s short name is used'
);
select tests.authenticate_as('a@test.dev');
select lives_ok($$select public.set_partner_nickname(null)$$, 'clearing a nickname that isn''t set is harmless');

select * from finish();
rollback;
