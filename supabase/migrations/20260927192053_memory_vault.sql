-- Ours (FR-VLT, FR-MEM): the live memory list and the Lock Ours setting.

-- New, viewed and kept memories reach the partner live (FR-VLT-1, FR-VLT-3).
-- Realtime applies the memories RLS policy to each subscriber.
alter publication supabase_realtime add table public.memories;

-- Lock Ours (FR-VLT-5, FR-SET-2): on by default, saved per user so it follows them
-- to a new phone. Users edit it through the existing "profiles: update own" policy.
alter table public.profiles add column lock_ours boolean not null default true;
