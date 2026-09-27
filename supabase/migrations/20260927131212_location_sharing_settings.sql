-- Location sharing settings live in the user's locations row (SRS section 5), so the row
-- must exist before the first position is known: coordinates become optional.
-- Turning sharing off removes the stored position (BR-3), and updated_at only moves when
-- the position changes, so a settings change never makes an old position look live (BR-8).

alter table public.locations
    alter column lat drop not null,
    alter column lng drop not null,
    add constraint locations_coordinates_together check ((lat is null) = (lng is null));

create or replace function private.round_approximate_location()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.precision = 'approximate' and new.lat is not null then
        new.lat := round(new.lat::numeric, 2)::double precision;
        new.lng := round(new.lng::numeric, 2)::double precision;
        new.accuracy_m := greatest(coalesce(new.accuracy_m, 0), 1000);
    end if;
    return new;
end;
$$;

create function private.clear_location_when_not_shared()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if not new.sharing_enabled then
        new.lat := null;
        new.lng := null;
        new.accuracy_m := null;
    end if;
    return new;
end;
$$;

-- Runs before the rounding trigger (same-event triggers fire in name order).
create trigger locations_clear_when_not_shared
    before insert or update on public.locations
    for each row execute function private.clear_location_when_not_shared();

create function private.touch_location_time()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if tg_op = 'INSERT' or new.lat is distinct from old.lat or new.lng is distinct from old.lng then
        new.updated_at := now();
    else
        new.updated_at := old.updated_at;
    end if;
    return new;
end;
$$;

-- Named to run after locations_clear_when_not_shared and before locations_round_approximate,
-- so rounding an unchanged position (e.g. switching to approximate) doesn't count as new.
drop trigger locations_touch on public.locations;
create trigger locations_position_time
    before insert or update on public.locations
    for each row execute function private.touch_location_time();

revoke all on function private.clear_location_when_not_shared() from public;
revoke all on function private.touch_location_time() from public;
