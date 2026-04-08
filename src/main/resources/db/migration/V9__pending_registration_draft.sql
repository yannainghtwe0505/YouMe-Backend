-- Defer user row creation until onboarding is completed: hold password + profile draft on pending row.

alter table pending_registrations add column if not exists password_hash varchar(255);
alter table pending_registrations add column if not exists onboarding_step varchar(40);
alter table pending_registrations add column if not exists tos_accepted_at timestamptz;
alter table pending_registrations add column if not exists privacy_accepted_at timestamptz;
alter table pending_registrations add column if not exists profile_draft jsonb;
