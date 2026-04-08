-- Multi-step onboarding: pending verification, optional phone, completion flag.

create table if not exists pending_registrations (
  id bigserial primary key,
  email varchar(255),
  phone_e164 varchar(32),
  channel varchar(16) not null,
  code_hash varchar(255) not null,
  expires_at timestamptz not null,
  attempts int not null default 0,
  session_token varchar(64) unique,
  session_expires_at timestamptz,
  created_at timestamptz not null default now(),
  constraint pending_one_contact check (
    (email is not null and phone_e164 is null)
    or (email is null and phone_e164 is not null)
  )
);

create unique index if not exists uq_pending_email on pending_registrations (email) where email is not null;
create unique index if not exists uq_pending_phone on pending_registrations (phone_e164) where phone_e164 is not null;

alter table users alter column email drop not null;

alter table users
  add column if not exists phone_e164 varchar(32);
create unique index if not exists uq_users_phone_e164 on users (phone_e164) where phone_e164 is not null;

alter table users
  add column if not exists registration_complete boolean not null default true;
alter table users
  add column if not exists onboarding_step varchar(40);
alter table users
  add column if not exists tos_accepted_at timestamptz;
alter table users
  add column if not exists privacy_accepted_at timestamptz;

alter table users
  add constraint users_login_identifier check (email is not null or phone_e164 is not null);
