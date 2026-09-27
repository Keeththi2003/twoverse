-- Private bucket for memory and birthday photos, one folder per couple:
-- memories/{couple_id}/... (NFR-SEC-2). Files are only served through signed URLs.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'memories',
    'memories',
    false,
    10485760,
    array['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/heif']
)
on conflict (id) do update
set public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

-- Upload into the caller's active couple folder, as themselves.
create policy "memories bucket: couple uploads"
    on storage.objects for insert to authenticated
    with check (
        bucket_id = 'memories'
        and (storage.foldername(name))[1] = (select private.active_couple_id())::text
        and owner_id = (select auth.uid())::text
    );

-- Read (and so create signed URLs for) a file only while its memory or birthday
-- welcome is readable: same couple, not expired, not hidden (BR-6). The subqueries
-- run with the caller's rights, so the table policies apply.
create policy "memories bucket: couple reads visible files"
    on storage.objects for select to authenticated
    using (
        bucket_id = 'memories'
        and (storage.foldername(name))[1] = (select private.active_couple_id())::text
        and (
            exists (select 1 from public.memories m where m.storage_path = objects.name)
            or exists (select 1 from public.birthday_welcomes b where b.photo_path = objects.name)
        )
    );

-- The uploader may remove their own file, e.g. after a failed send (FR-MEM-6).
create policy "memories bucket: uploader deletes own file"
    on storage.objects for delete to authenticated
    using (
        bucket_id = 'memories'
        and (storage.foldername(name))[1] = (select private.active_couple_id())::text
        and owner_id = (select auth.uid())::text
    );
