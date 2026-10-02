begin;
select plan(3);

select is_empty(
    $$select n.nspname || '.' || c.relname
      from pg_class c join pg_namespace n on n.oid = c.relnamespace
      where c.relkind in ('r', 'p') and n.nspname in ('public', 'private') and not c.relrowsecurity$$,
    'every table in public and private has Row Level Security (NFR-SEC-1)'
);

select is_empty(
    $$select table_schema || '.' || table_name || ' ' || grantee
      from information_schema.role_table_grants
      where table_schema = 'private' and grantee in ('anon', 'authenticated')$$,
    'clients have no direct access to private tables'
);

select is_empty(
    $$select id from storage.buckets where public$$,
    'every storage bucket is private (NFR-SEC-2)'
);

select * from finish();
rollback;
