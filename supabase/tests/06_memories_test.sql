begin;
select plan(23);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;

-- Seed memories A sent to B (as the superuser, including past ones)
insert into public.memories (id, couple_id, sender_id, storage_path, expires_at, allow_keep, created_at)
select id::uuid, (select ab from ids), tests.user_id('a@test.dev'), (select ab from ids)::text || '/' || name, expires_at, allow_keep, created_at
from (values
    ('00000000-0000-0000-0000-000000000001', 'forever.jpg', null::timestamptz, false, now()),
    ('00000000-0000-0000-0000-000000000002', 'keepable.jpg', now() + interval '2 days', true, now()),
    ('00000000-0000-0000-0000-000000000003', 'temporary.jpg', now() + interval '1 day', false, now()),
    ('00000000-0000-0000-0000-000000000004', 'expired.jpg', now() - interval '1 minute', true, now() - interval '2 days')
) as seed(id, name, expires_at, allow_keep, created_at);

-- Insert (FR-MEM)
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.memories (couple_id, sender_id, storage_path, caption) values (%L, %L, %L, 'hi')$$,
        (select ab from ids), tests.user_id('a@test.dev'), (select ab from ids)::text || '/new.jpg'),
    'a member can add a memory they send'
);
select throws_ok(
    format($$insert into public.memories (couple_id, sender_id, storage_path) values (%L, %L, %L)$$,
        (select ab from ids), tests.user_id('b@test.dev'), (select ab from ids)::text || '/fake.jpg'),
    '42501', null,
    'a user cannot add a memory as their partner'
);
select throws_ok(
    format($$insert into public.memories (couple_id, sender_id, storage_path) values (%L, %L, %L)$$,
        (select cd from ids), tests.user_id('a@test.dev'), (select cd from ids)::text || '/x.jpg'),
    '42501', null,
    'a user cannot add a memory to another couple'
);
select throws_ok(
    format($$insert into public.memories (couple_id, sender_id, storage_path, expires_at, created_at) values (%L, %L, %L, now() - interval '1 hour', now() - interval '2 hours')$$,
        (select ab from ids), tests.user_id('a@test.dev'), (select ab from ids)::text || '/past.jpg'),
    '42501', null,
    'a memory cannot be added already expired'
);

-- Read: couple only, never expired (BR-6)
select tests.authenticate_as('b@test.dev');
select is((select count(*)::int from public.memories), 4, 'the partner reads the couple''s unexpired memories');
select is_empty($$select 1 from public.memories where storage_path like '%expired.jpg'$$, 'expired memories are unreadable (BR-6)');
select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from public.memories', 'another couple cannot read memories');

-- mark_memory_viewed (FR-VLT-3)
select tests.authenticate_as('b@test.dev');
select lives_ok($$select public.mark_memory_viewed('00000000-0000-0000-0000-000000000001')$$, 'the recipient marks a memory viewed');
select tests.authenticate_as('a@test.dev');
select lives_ok($$select public.mark_memory_viewed('00000000-0000-0000-0000-000000000003')$$, 'the sender opening it changes nothing');
select tests.authenticate_as('c@test.dev');
select throws_ok($$select public.mark_memory_viewed('00000000-0000-0000-0000-000000000001')$$, 'P0001', 'memory_not_found', 'another couple cannot mark it');
select tests.clear_authentication();
select results_eq(
    $$select id::text, viewed_at is not null from public.memories where id in ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003') order by id$$,
    $$values ('00000000-0000-0000-0000-000000000001', true), ('00000000-0000-0000-0000-000000000003', false)$$,
    'only the recipient''s view is recorded'
);

-- keep_memory_forever (FR-MEM-5, FR-DEL-2)
select tests.authenticate_as('a@test.dev');
select throws_ok($$select public.keep_memory_forever('00000000-0000-0000-0000-000000000002')$$, 'P0001', 'not_recipient', 'the sender cannot keep their own memory');
select tests.authenticate_as('b@test.dev');
select throws_ok($$select public.keep_memory_forever('00000000-0000-0000-0000-000000000003')$$, 'P0001', 'keep_not_allowed', 'keeping needs the sender''s permission');
select throws_ok($$select public.keep_memory_forever('00000000-0000-0000-0000-000000000004')$$, 'P0001', 'memory_not_found', 'an expired memory cannot be kept');
select lives_ok($$select public.keep_memory_forever('00000000-0000-0000-0000-000000000002')$$, 'the recipient keeps an allowed memory');
select is((select expires_at from public.memories where id = '00000000-0000-0000-0000-000000000002'), null, 'keeping removes the expiry');

-- hide_memory: the recipient hides, the sender still sees it (SRS 12)
select throws_ok($$select public.hide_memory(gen_random_uuid())$$, 'P0001', 'memory_not_found', 'unknown memories cannot be hidden');
select lives_ok($$select public.hide_memory('00000000-0000-0000-0000-000000000003')$$, 'the recipient hides a memory');
select is_empty($$select 1 from public.memories where id = '00000000-0000-0000-0000-000000000003'$$, 'a hidden memory leaves the recipient''s vault');
select tests.authenticate_as('a@test.dev');
select isnt_empty($$select 1 from public.memories where id = '00000000-0000-0000-0000-000000000003'$$, 'the sender still sees it');
select throws_ok($$select public.hide_memory('00000000-0000-0000-0000-000000000001')$$, 'P0001', 'not_recipient', 'the sender cannot hide their own memory');

-- Delete: sender only, file queued for deletion (FR-DEL-3, SRS 12)
select tests.authenticate_as('b@test.dev');
delete from public.memories where id = '00000000-0000-0000-0000-000000000001';
update public.memories set caption = 'changed', expires_at = null;
select tests.authenticate_as('c@test.dev');
delete from public.memories;
select tests.clear_authentication();
select results_eq(
    $$select count(*)::int, count(*) filter (where caption = 'changed')::int from public.memories where id = '00000000-0000-0000-0000-000000000001'$$,
    $$values (1, 0)$$,
    'the recipient and other couples cannot delete or edit memories'
);
select tests.authenticate_as('a@test.dev');
delete from public.memories where id = '00000000-0000-0000-0000-000000000001';
select tests.clear_authentication();
select results_eq(
    $$select object_path from private.storage_deletion_queue$$,
    format($$values (%L)$$, (select ab from ids)::text || '/forever.jpg'),
    'the sender deletes a memory for both, and its file is queued for deletion'
);

select * from finish();
rollback;
