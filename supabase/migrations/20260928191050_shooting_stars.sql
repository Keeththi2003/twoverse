-- Shooting Stars (FR-STAR): surprise messages one partner prepares for the other, shown once
-- on a later app open. They replace the single birthday welcome (FR-BDY, SRS 2.0); existing
-- birthday welcomes are carried over as Shooting Stars.
--
--   * only the couple reads a star; the recipient only once it is visible (FR-STAR-15)
--   * only the sender creates, edits or deletes it, and only while it is unseen
--   * the recipient can only mark it seen, through mark_shooting_star_seen()
--   * photos live in the memories bucket under {couple_id}/stars/ (FR-STAR-16)
--   * a push is sent when a star becomes visible, by trigger or pg_cron (FR-STAR-17)
--
-- Errors: not_authenticated, star_not_found, invalid_photo_path

-- ---------------------------------------------------------------------------
-- shooting_stars
-- ---------------------------------------------------------------------------
create table public.shooting_stars (
    id uuid primary key default gen_random_uuid(),
    couple_id uuid not null references public.couples (id) on delete cascade,
    sender_id uuid not null references public.profiles (id) on delete cascade,
    recipient_id uuid not null references public.profiles (id) on delete cascade,
    layout text not null check (layout in ('photo_message', 'message_only', 'full_photo')),
    -- Empty fields are stored as null, so the viewer can hide them (FR-STAR-10).
    eyebrow text check (char_length(eyebrow) <= 40 and btrim(eyebrow) <> ''),
    title text check (char_length(title) <= 80 and btrim(title) <> ''),
    message text check (char_length(message) <= 1000 and btrim(message) <> ''),
    signature text check (char_length(signature) <= 50 and btrim(signature) <> ''),
    photo_path text,
    photo_fit text not null default 'fill' check (photo_fit in ('fill', 'fit')),
    -- Null: shown the next time the recipient opens the app (FR-STAR-7).
    show_at timestamptz,
    seen_at timestamptz,
    created_at timestamptz not null default now(),
    constraint shooting_stars_not_for_self check (recipient_id <> sender_id),
    constraint shooting_stars_photo_in_couple_folder check (photo_path is null or photo_path like couple_id::text || '/%'),
    -- Each layout uses only its own fields and must show something (FR-STAR-4, FR-STAR-5).
    constraint shooting_stars_layout_fields check (
        case layout
            when 'full_photo' then photo_path is not null and eyebrow is null and signature is null
            when 'message_only' then photo_path is null and (title is not null or message is not null)
            else photo_path is not null or title is not null or message is not null
        end
    )
);

create index shooting_stars_couple_idx on public.shooting_stars (couple_id);
create index shooting_stars_recipient_idx on public.shooting_stars (recipient_id, created_at);
create index shooting_stars_sender_idx on public.shooting_stars (sender_id, created_at desc);
create index shooting_stars_unseen_idx on public.shooting_stars (show_at) where seen_at is null;

-- ---------------------------------------------------------------------------
-- Existing birthday welcomes become Photo-and-message stars with the birthday template.
-- A date meant "from midnight on that day" in the recipient's time zone.
-- ---------------------------------------------------------------------------
insert into public.shooting_stars (
    couple_id, sender_id, recipient_id, layout, eyebrow, title, message, signature,
    photo_path, show_at, seen_at, created_at
)
select
    b.couple_id,
    b.created_by,
    b.for_user_id,
    'photo_message',
    'For you',
    'Happy Birthday',
    btrim(b.message),
    left(sender.display_name, 50),
    b.photo_path,
    b.show_on::timestamp at time zone recipient.time_zone,
    b.seen_at,
    b.created_at
from public.birthday_welcomes b
join public.profiles sender on sender.id = b.created_by
join public.profiles recipient on recipient.id = b.for_user_id;

-- ---------------------------------------------------------------------------
-- Replace the birthday welcome in storage access, then remove it.
-- ---------------------------------------------------------------------------
drop policy "memories bucket: couple reads visible files" on storage.objects;

-- Read (and so create signed URLs for) a file only while its memory or Shooting Star is
-- readable: same couple, memory not expired or hidden (BR-6), star visible to the caller
-- (FR-STAR-15). The subqueries run with the caller's rights, so the table policies apply.
create policy "memories bucket: couple reads visible files"
    on storage.objects for select to authenticated
    using (
        bucket_id = 'memories'
        and (storage.foldername(name))[1] = (select private.active_couple_id())::text
        and (
            exists (select 1 from public.memories m where m.storage_path = objects.name)
            or exists (select 1 from public.shooting_stars s where s.photo_path = objects.name)
        )
    );

drop function public.mark_birthday_welcome_seen();
drop table public.birthday_welcomes;
drop function private.queue_birthday_photo_deletion();

-- ---------------------------------------------------------------------------
-- Photos: new ones must be in the couple's stars folder, so a star can't expose another
-- file such as an expired memory (BR-6). Carried-over birthday photos keep their path.
-- ---------------------------------------------------------------------------
create function private.check_shooting_star_photo_folder()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.photo_path is not null
       and (tg_op = 'INSERT' or new.photo_path is distinct from old.photo_path)
       and new.photo_path not like new.couple_id::text || '/stars/%' then
        raise exception 'invalid_photo_path' using errcode = 'P0001';
    end if;
    return new;
end;
$$;

create trigger shooting_stars_photo_folder
    before insert or update of photo_path on public.shooting_stars
    for each row execute function private.check_shooting_star_photo_folder();

-- A replaced or deleted photo is removed from storage by the purge job (FR-STAR-16).
create function private.queue_shooting_star_photo_deletion()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if old.photo_path is not null and (tg_op = 'DELETE' or new.photo_path is distinct from old.photo_path) then
        insert into private.storage_deletion_queue (bucket_id, object_path)
        values ('memories', old.photo_path);
    end if;
    return coalesce(new, old);
end;
$$;

create trigger shooting_stars_queue_photo_deletion
    after update of photo_path or delete on public.shooting_stars
    for each row execute function private.queue_shooting_star_photo_deletion();

-- ---------------------------------------------------------------------------
-- Row Level Security (FR-STAR-15, NFR-SEC-1)
-- ---------------------------------------------------------------------------
alter table public.shooting_stars enable row level security;

-- Clients change only the content; seen_at goes through mark_shooting_star_seen().
revoke all on public.shooting_stars from anon, authenticated;
grant select, delete on public.shooting_stars to authenticated;
grant insert (couple_id, sender_id, recipient_id, layout, eyebrow, title, message, signature, photo_path, photo_fit, show_at)
    on public.shooting_stars to authenticated;
grant update (layout, eyebrow, title, message, signature, photo_path, photo_fit, show_at)
    on public.shooting_stars to authenticated;

create policy "shooting_stars: sender reads own"
    on public.shooting_stars for select to authenticated
    using (couple_id = (select private.active_couple_id()) and sender_id = (select auth.uid()));

-- A scheduled star stays a surprise until its time (FR-STAR-7).
create policy "shooting_stars: recipient reads visible"
    on public.shooting_stars for select to authenticated
    using (
        couple_id = (select private.active_couple_id())
        and recipient_id = (select auth.uid())
        and (show_at is null or show_at <= now())
    );

create policy "shooting_stars: sender creates"
    on public.shooting_stars for insert to authenticated
    with check (
        couple_id = (select private.active_couple_id())
        and sender_id = (select auth.uid())
        and recipient_id = (select private.partner_id())
    );

create policy "shooting_stars: sender edits while unseen"
    on public.shooting_stars for update to authenticated
    using (couple_id = (select private.active_couple_id()) and sender_id = (select auth.uid()) and seen_at is null)
    with check (
        couple_id = (select private.active_couple_id())
        and sender_id = (select auth.uid())
        and recipient_id = (select private.partner_id())
        and seen_at is null
    );

create policy "shooting_stars: sender deletes while unseen"
    on public.shooting_stars for delete to authenticated
    using (couple_id = (select private.active_couple_id()) and sender_id = (select auth.uid()) and seen_at is null);

-- ---------------------------------------------------------------------------
-- mark_shooting_star_seen(id): the recipient opened it, so it shows once (FR-STAR-12).
-- ---------------------------------------------------------------------------
create function public.mark_shooting_star_seen(p_star_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    me uuid := private.require_user();
begin
    update public.shooting_stars
    set seen_at = coalesce(seen_at, now())
    where id = p_star_id
      and couple_id = private.active_couple_id()
      and recipient_id = me
      and (show_at is null or show_at <= now());
    if not found then
        raise exception 'star_not_found' using errcode = 'P0001';
    end if;
end;
$$;

revoke all on function public.mark_shooting_star_seen(uuid) from public, anon;
grant execute on function public.mark_shooting_star_seen(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- Push when a star becomes visible (FR-STAR-17). Each star is claimed once; the recipient
-- gets at most one push a minute, however many stars arrive. Uses the same Vault secrets
-- as the reunion-day job (project_url, push_internal_secret); until they exist it waits.
-- ---------------------------------------------------------------------------
create table private.shooting_star_notifications (
    star_id uuid primary key references public.shooting_stars (id) on delete cascade,
    recipient_id uuid not null references public.profiles (id) on delete cascade,
    notified_at timestamptz not null default now()
);

create index shooting_star_notifications_recipient_idx
    on private.shooting_star_notifications (recipient_id, notified_at);

alter table private.shooting_star_notifications enable row level security;

-- Claims every visible, unseen star of an active couple not claimed before, and returns
-- the recipients to notify: those without a push in the last minute.
create function private.claim_shooting_star_recipients()
returns uuid[]
language sql
security definer
set search_path = ''
as $$
    with due as (
        select s.id, s.recipient_id
        from public.shooting_stars s
        join public.couples c on c.id = s.couple_id and c.status = 'active'
        where s.seen_at is null
          and (s.show_at is null or s.show_at <= now())
          and not exists (select 1 from private.shooting_star_notifications n where n.star_id = s.id)
    ),
    claimed as (
        insert into private.shooting_star_notifications (star_id, recipient_id)
        select id, recipient_id from due
        on conflict do nothing
        returning recipient_id
    )
    -- The claims above aren't visible yet in this statement, so this sees earlier pushes only.
    select coalesce(array_agg(distinct c.recipient_id), '{}')
    from claimed c
    where not exists (
        select 1 from private.shooting_star_notifications n
        where n.recipient_id = c.recipient_id and n.notified_at > now() - interval '1 minute'
    );
$$;

create function private.send_shooting_star_notifications()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    project_url text;
    internal_secret text;
    recipients uuid[];
begin
    select decrypted_secret into project_url from vault.decrypted_secrets where name = 'project_url';
    select decrypted_secret into internal_secret from vault.decrypted_secrets where name = 'push_internal_secret';
    if project_url is null or internal_secret is null then
        return;
    end if;

    recipients := private.claim_shooting_star_recipients();
    if cardinality(recipients) = 0 then
        return;
    end if;

    perform net.http_post(
        url := rtrim(project_url, '/') || '/functions/v1/send-push',
        headers := jsonb_build_object('Content-Type', 'application/json', 'x-internal-secret', internal_secret),
        body := jsonb_build_object('type', 'shooting_star', 'user_ids', to_jsonb(recipients))
    );
end;
$$;

-- Sent for the next open: notify right away (pg_net sends after the transaction commits).
-- Moved to a later time: notify again when that time comes.
create function private.on_shooting_star_scheduled()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.show_at is not null and new.show_at > now() then
        delete from private.shooting_star_notifications where star_id = new.id;
    else
        perform private.send_shooting_star_notifications();
    end if;
    return null;
end;
$$;

create trigger shooting_stars_notify
    after insert or update of show_at on public.shooting_stars
    for each row execute function private.on_shooting_star_scheduled();

-- Carried-over welcomes that are already visible or seen don't trigger a push now.
insert into private.shooting_star_notifications (star_id, recipient_id)
select id, recipient_id from public.shooting_stars
where seen_at is not null or show_at is null or show_at <= now();

revoke all on function private.check_shooting_star_photo_folder() from public;
revoke all on function private.queue_shooting_star_photo_deletion() from public;
revoke all on function private.claim_shooting_star_recipients() from public;
revoke all on function private.send_shooting_star_notifications() from public;
revoke all on function private.on_shooting_star_scheduled() from public;

select cron.schedule(
    'twoverse-shooting-star-notifications',
    '* * * * *',
    'select private.send_shooting_star_notifications()'
);
