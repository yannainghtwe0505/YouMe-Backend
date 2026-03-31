-- =============================================================================
-- Dating app — full PostgreSQL schema (matches JPA entities exactly)
-- =============================================================================
-- NEW empty database: run this file once.
-- EXISTING broken/partial schema (dev reset): use recreate-schema.sql instead.
--
--   psql -U DatingAppDB -d DatingAppDB -f scripts/init-postgresql.sql
-- =============================================================================

BEGIN;

create table if not exists users (
  id bigserial primary key,
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  created_at timestamptz not null default now(),
  last_login timestamptz
);

create table if not exists profiles (
  user_id bigint primary key references users(id) on delete cascade,
  display_name varchar(100),
  bio varchar(2000),
  gender varchar(20),
  birthday date,
  interests jsonb,
  latitude double precision,
  longitude double precision,
  min_age int,
  max_age int,
  distance_km int,
  city varchar(255),
  education varchar(200),
  occupation varchar(200),
  hobbies varchar(500),
  photo_url varchar(1024),
  is_premium boolean not null default false
);

create table if not exists photos (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  s3_key varchar(512) not null,
  is_primary boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists likes (
  id bigserial primary key,
  from_user bigint not null references users(id) on delete cascade,
  to_user bigint not null references users(id) on delete cascade,
  super_like boolean not null default false,
  created_at timestamptz not null default now(),
  unique(from_user, to_user)
);

create table if not exists passes (
  id bigserial primary key,
  from_user bigint not null references users(id) on delete cascade,
  to_user bigint not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique(from_user, to_user)
);

create table if not exists matches (
  id bigserial primary key,
  user_a bigint not null references users(id) on delete cascade,
  user_b bigint not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint uq_pair unique (user_a, user_b)
);

create table if not exists messages (
  id bigserial primary key,
  match_id bigint not null references matches(id) on delete cascade,
  sender_id bigint not null references users(id) on delete cascade,
  body varchar(2000) not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_messages_match_created on messages(match_id, created_at);

COMMIT;
