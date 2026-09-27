-- Test helpers shared by every file in this folder (local database only).
-- pg_prove runs files in name order, so this one installs them first.
create extension if not exists pgtap with schema extensions;

create schema if not exists tests;
grant usage on schema tests to anon, authenticated;

-- Creates an auth user (the sign-up trigger adds the profile) and returns its id.
create or replace function tests.create_user(email text, metadata jsonb default '{}')
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    user_id uuid := gen_random_uuid();
begin
    insert into auth.users (id, instance_id, aud, role, email, raw_user_meta_data, created_at, updated_at)
    values (user_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', email, metadata, now(), now());
    return user_id;
end;
$$;

create or replace function tests.user_id(email text)
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select u.id from auth.users u where u.email = user_id.email;
$$;

-- An active couple created directly (bypassing the pairing RPC), for policy tests.
create or replace function tests.create_couple(first_user uuid, second_user uuid)
returns uuid
language sql
security definer
set search_path = ''
as $$
    insert into public.couples (user_a, user_b, status, connected_at)
    values (first_user, second_user, 'active', now())
    returning id;
$$;

-- Acts as the given user for the rest of the transaction (like a signed-in client).
create or replace function tests.authenticate_as(email text)
returns void
language plpgsql
as $$
begin
    perform set_config('request.jwt.claims', json_build_object('sub', tests.user_id(email), 'role', 'authenticated')::text, true);
    perform set_config('role', 'authenticated', true);
end;
$$;

-- Back to the superuser used for setup and checks that bypass RLS.
create or replace function tests.clear_authentication()
returns void
language plpgsql
as $$
begin
    perform set_config('role', 'postgres', true);
    perform set_config('request.jwt.claims', null, true);
end;
$$;

grant execute on all functions in schema tests to anon, authenticated;

begin;
select plan(1);
select has_function('tests', 'authenticate_as', array['text'], 'test helpers are installed');
select * from finish();
rollback;
