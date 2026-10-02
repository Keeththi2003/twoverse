-- Defence in depth (NFR-SEC-1): the private schema isn't exposed through the API and grants
-- clients no table access, but every table still gets Row Level Security. No policies are
-- added, so only the table owner (the security definer functions and pg_cron jobs) can read
-- or change these rows.
alter table private.push_rate_limits enable row level security;
alter table private.reunion_day_notifications enable row level security;
alter table private.storage_deletion_queue enable row level security;
