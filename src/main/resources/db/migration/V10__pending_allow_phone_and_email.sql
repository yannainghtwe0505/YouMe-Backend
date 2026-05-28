-- Allow optional email alongside phone on the same pending row (phone signup + optional email).

alter table pending_registrations drop constraint if exists pending_one_contact;
do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'pending_has_contact'
      and conrelid = 'pending_registrations'::regclass
  ) then
    alter table pending_registrations add constraint pending_has_contact check (
      email is not null or phone_e164 is not null
    );
  end if;
end$$;
