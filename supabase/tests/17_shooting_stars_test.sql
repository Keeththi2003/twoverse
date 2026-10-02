begin;
select plan(34);

select tests.create_user('a@test.dev');
select tests.create_user('b@test.dev');
select tests.create_user('c@test.dev');
select tests.create_user('d@test.dev');
create temporary table ids as
select tests.create_couple(tests.user_id('a@test.dev'), tests.user_id('b@test.dev')) as ab,
       tests.create_couple(tests.user_id('c@test.dev'), tests.user_id('d@test.dev')) as cd;
grant select on ids to authenticated;

-- Creating (FR-STAR-1, FR-STAR-15): only the sender, for their partner, in their couple
select tests.authenticate_as('a@test.dev');
select lives_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title, photo_path)
             values (%L, %L, %L, 'photo_message', 'Now', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), (select ab from ids)::text || '/stars/now.jpg'),
    'a user sends a star to their partner for the next open'
);
select lives_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title, photo_path, show_at)
             values (%L, %L, %L, 'photo_message', 'Later', %L, now() + interval '1 day')$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), (select ab from ids)::text || '/stars/later.jpg'),
    'a user schedules a star for later'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title) values (%L, %L, %L, 'message_only', 'x')$$,
        (select cd from ids), tests.user_id('a@test.dev'), tests.user_id('c@test.dev')),
    '42501', null,
    'a user cannot send a star into another couple'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title) values (%L, %L, %L, 'message_only', 'x')$$,
        (select ab from ids), tests.user_id('b@test.dev'), tests.user_id('a@test.dev')),
    '42501', null,
    'a user cannot send a star as their partner'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title, seen_at) values (%L, %L, %L, 'message_only', 'x', now())$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    '42501', null,
    'a star cannot be created already seen'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, photo_path) values (%L, %L, %L, 'full_photo', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), (select ab from ids)::text || '/memory.jpg'),
    'P0001', 'invalid_photo_path',
    'a star photo must be in the couple''s stars folder, so it cannot expose a memory file'
);

-- Layout rules (FR-STAR-4, FR-STAR-5)
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title) values (%L, %L, %L, 'full_photo', 'x')$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    '23514', null,
    'a full photo star needs a photo'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title, photo_path) values (%L, %L, %L, 'message_only', 'x', %L)$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), (select ab from ids)::text || '/stars/x.jpg'),
    '23514', null,
    'a message-only star has no photo'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, eyebrow) values (%L, %L, %L, 'message_only', 'For you')$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    '23514', null,
    'a message-only star needs a title or message'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, message) values (%L, %L, %L, 'message_only', repeat('x', 1001))$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    '23514', null,
    'messages are limited to 1000 characters'
);
select throws_ok(
    format($$insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title, message) values (%L, %L, %L, 'message_only', '  ', 'hi')$$,
        (select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev')),
    '23514', null,
    'empty fields are stored as null, never as blank text'
);

select tests.clear_authentication();
create temporary table stars as select title, id from public.shooting_stars;
grant select on stars to authenticated;

-- Reading: the couple only; the recipient only once visible (FR-STAR-15)
select tests.authenticate_as('b@test.dev');
select results_eq('select title from public.shooting_stars', $$values ('Now'::text)$$, 'the recipient reads visible stars, not scheduled ones');
select tests.authenticate_as('a@test.dev');
select results_eq('select title from public.shooting_stars order by title', $$values ('Later'::text), ('Now')$$, 'the sender reads all of theirs');
select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from public.shooting_stars', 'another couple cannot read them');
select set_config('role', 'anon', true);
select throws_ok('select 1 from public.shooting_stars', '42501', null, 'signed-out clients cannot read them');

-- The recipient can only mark a star seen (FR-STAR-12)
select tests.authenticate_as('b@test.dev');
update public.shooting_stars set title = 'hacked';
delete from public.shooting_stars;
select throws_ok('update public.shooting_stars set seen_at = now()', '42501', null, 'the recipient cannot set seen_at directly');
select throws_ok(
    format('select public.mark_shooting_star_seen(%L)', (select id from stars where title = 'Later')),
    'P0001', 'star_not_found',
    'a scheduled star cannot be marked seen before its time'
);
select lives_ok(
    format('select public.mark_shooting_star_seen(%L)', (select id from stars where title = 'Now')),
    'the recipient marks a visible star seen'
);
select tests.authenticate_as('c@test.dev');
select throws_ok(
    format('select public.mark_shooting_star_seen(%L)', (select id from stars where title = 'Now')),
    'P0001', 'star_not_found',
    'another couple cannot mark it seen'
);

-- The sender edits and deletes only while unseen (FR-STAR-9)
select tests.authenticate_as('a@test.dev');
select throws_ok(
    format('select public.mark_shooting_star_seen(%L)', (select id from stars where title = 'Later')),
    'P0001', 'star_not_found',
    'the sender cannot mark their own star seen'
);
update public.shooting_stars set title = 'edited after seen' where title = 'Now';
delete from public.shooting_stars where title = 'Now';
select lives_ok(
    $$update public.shooting_stars set title = 'Later, edited' where title = 'Later'$$,
    'the sender edits an unseen star'
);
select tests.authenticate_as('c@test.dev');
update public.shooting_stars set title = 'c';
delete from public.shooting_stars;
select tests.clear_authentication();
select results_eq(
    'select title, seen_at is not null from public.shooting_stars order by title',
    $$values ('Later, edited'::text, false), ('Now', true)$$,
    'only the sender changes unseen stars; the recipient''s seen mark is kept'
);

-- Photos (FR-STAR-16): readable only while the star is readable
insert into storage.objects (bucket_id, name, owner_id)
values ('memories', (select ab from ids)::text || '/stars/now.jpg', tests.user_id('a@test.dev')),
       ('memories', (select ab from ids)::text || '/stars/later.jpg', tests.user_id('a@test.dev'));
select tests.authenticate_as('b@test.dev');
select results_eq(
    'select name from storage.objects',
    format('values (%L)', (select ab from ids)::text || '/stars/now.jpg'),
    'the recipient reads a visible star''s photo, not a scheduled one''s'
);
select tests.authenticate_as('c@test.dev');
select is_empty('select 1 from storage.objects', 'another couple cannot read star photos');

select tests.authenticate_as('a@test.dev');
select lives_ok($$delete from public.shooting_stars where title = 'Later, edited'$$, 'the sender deletes an unseen star');
select tests.clear_authentication();
select results_eq(
    'select object_path from private.storage_deletion_queue',
    format('values (%L)', (select ab from ids)::text || '/stars/later.jpg'),
    'a deleted star''s photo is queued for deletion'
);

-- Push when a star becomes visible (FR-STAR-17)
insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title, show_at)
values ((select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), 'message_only', 'N1', null),
       ((select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), 'message_only', 'N2', null),
       ((select cd from ids), tests.user_id('c@test.dev'), tests.user_id('d@test.dev'), 'message_only', 'N3', now() + interval '1 hour');
select is(
    private.claim_shooting_star_recipients(),
    array[tests.user_id('b@test.dev')],
    'the recipient of visible stars is notified once, not for seen or scheduled stars'
);
select is(cardinality(private.claim_shooting_star_recipients()), 0, 'nobody is notified twice');

update public.shooting_stars set show_at = now() - interval '1 second' where title = 'N3';
select is(
    private.claim_shooting_star_recipients(),
    array[tests.user_id('d@test.dev')],
    'a scheduled star is notified when its time comes'
);

insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title)
values ((select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), 'message_only', 'N4');
select is(cardinality(private.claim_shooting_star_recipients()), 0, 'another star within a minute sends no second push');

update private.shooting_star_notifications set notified_at = now() - interval '2 minutes';
insert into public.shooting_stars (couple_id, sender_id, recipient_id, layout, title)
values ((select ab from ids), tests.user_id('a@test.dev'), tests.user_id('b@test.dev'), 'message_only', 'N5');
select is(
    private.claim_shooting_star_recipients(),
    array[tests.user_id('b@test.dev')],
    'after a minute the recipient is notified again'
);

update public.shooting_stars set show_at = now() + interval '1 day' where title = 'N1';
select is_empty(
    $$select 1 from private.shooting_star_notifications n join public.shooting_stars s on s.id = n.star_id where s.title = 'N1'$$,
    'a star moved to a later time is notified again when that time comes'
);

select tests.authenticate_as('a@test.dev');
select throws_ok('select private.claim_shooting_star_recipients()', '42501', null, 'users cannot claim notifications');
select tests.clear_authentication();
select lives_ok('select private.send_shooting_star_notifications()', 'the scheduled job runs without Vault secrets (it waits)');

select * from finish();
rollback;
