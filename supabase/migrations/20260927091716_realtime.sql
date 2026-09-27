-- Live updates for pairing (FR-PAIR-6), partner location (FR-LOC-3) and the shared
-- countdown (FR-CNT-2). Realtime applies the tables' RLS policies to each subscriber.
alter publication supabase_realtime add table public.couples, public.locations, public.reunions;
