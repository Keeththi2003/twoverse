-- When the reunion date was chosen, so "Getting closer" measures the wait from that moment.
-- updated_at also moves when only the place or note changes, which would reset the progress.

alter table public.reunions add column date_set_at timestamptz not null default now();

create function private.track_reunion_date_set()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if tg_op = 'INSERT' or new.meet_at is distinct from old.meet_at then
        new.date_set_at := now();
    else
        new.date_set_at := old.date_set_at;
    end if;
    return new;
end;
$$;

create trigger reunions_date_set_at
    before insert or update on public.reunions
    for each row execute function private.track_reunion_date_set();

revoke all on function private.track_reunion_date_set() from public;
