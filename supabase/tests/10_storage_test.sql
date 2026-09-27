begin;
select plan(11);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;

select is((select public from storage.buckets where id = 'memories'), false, 'the memories bucket is private (NFR-SEC-2)');

-- Upload (FR-MEM)
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into storage.objects (bucket_id, name, owner_id) values ('memories', %L, %L)$$,
        (select ab from ids)::text || '/photo.jpg', tests.user_id('a@test.dev')),
    'a member uploads into their couple folder'
);
select throws_ok(
    format($$insert into storage.objects (bucket_id, name, owner_id) values ('memories', %L, %L)$$,
        (select cd from ids)::text || '/photo.jpg', tests.user_id('a@test.dev')),
    '42501', null,
    'a user cannot upload into another couple''s folder'
);
select throws_ok(
    format($$insert into storage.objects (bucket_id, name, owner_id) values ('memories', %L, %L)$$,
        (select ab from ids)::text || '/as-b.jpg', tests.user_id('b@test.dev')),
    '42501', null,
    'a user cannot upload as their partner'
);

-- Read only while the memory is readable (BR-6)
select tests.authenticate_as('b@test.dev');
select is_empty('select 1 from storage.objects', 'a file without a memory row is not readable');

select tests.clear_authentication();
insert into public.memories (couple_id, sender_id, storage_path) values ((select ab from ids), tests.user_id('a@test.dev'), (select ab from ids)::text || '/photo.jpg');
select tests.authenticate_as('b@test.dev');
select results_eq('select name from storage.objects', format('values (%L)', (select ab from ids)::text || '/photo.jpg'), 'the partner can read the memory''s file');
select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from storage.objects', 'another couple cannot read it');

select tests.clear_authentication();
update public.memories set created_at = now() - interval '2 days', expires_at = now() - interval '1 minute';
select tests.authenticate_as('b@test.dev');
select is_empty('select 1 from storage.objects', 'an expired memory''s file is unreadable before it is deleted');

-- Delete: the uploader only (Storage API sets storage.allow_delete_query)
select set_config('storage.allow_delete_query', 'true', true);
delete from storage.objects;
select tests.authenticate_as('c@test.dev');
delete from storage.objects;
select tests.clear_authentication();
select is((select count(*)::int from storage.objects where bucket_id = 'memories'), 1, 'the partner and other couples cannot delete the file');
select tests.authenticate_as('a@test.dev');
delete from storage.objects;
select tests.clear_authentication();
select is((select count(*)::int from storage.objects where bucket_id = 'memories'), 0, 'the uploader can delete their own file');

select tests.clear_authentication();
insert into storage.objects (bucket_id, name, owner_id)
values ('memories', (select ab from ids)::text || '/kept.jpg', tests.user_id('a@test.dev'));
select tests.authenticate_as('a@test.dev');
update storage.objects set name = (select cd from ids)::text || '/moved.jpg';
select tests.clear_authentication();
select results_eq(
    $$select name from storage.objects where bucket_id = 'memories'$$,
    format('values (%L)', (select ab from ids)::text || '/kept.jpg'),
    'files cannot be moved or overwritten'
);

select * from finish();
rollback;
