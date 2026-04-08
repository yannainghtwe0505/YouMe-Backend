-- Repair DBs where Hibernate added registration_complete without DEFAULT (existing rows = NULL, NOT NULL fails).
-- Safe if column is missing (e.g. V7 not applied yet): no-op.

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public'
      and table_name = 'users'
      and column_name = 'registration_complete'
  ) then
    update public.users set registration_complete = true where registration_complete is null;
    alter table public.users alter column registration_complete set default true;
    alter table public.users alter column registration_complete set not null;
  end if;
end $$;
