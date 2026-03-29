create table users (
  id bigserial primary key,
  email varchar(255) unique not null,
  password_hash varchar(255) not null,
  created_at timestamp not null default now(),
  last_login timestamp
);

create table profiles (
  user_id bigint primary key references users(id) on delete cascade,
  display_name varchar(100),
  bio text,
  gender varchar(20),
  birthday date,
  interests jsonb,
  latitude double precision,
  longitude double precision,
  min_age int,
  max_age int,
  distance_km int
);

create table photos (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  s3_key varchar(512) not null,
  is_primary boolean not null default false,
  created_at timestamp not null default now()
);

create table likes (
  id bigserial primary key,
  from_user bigint not null references users(id) on delete cascade,
  to_user bigint not null references users(id) on delete cascade,
  created_at timestamp not null default now(),
  unique(from_user, to_user)
);

create table matches (
  id bigserial primary key,
  user_a bigint not null references users(id) on delete cascade,
  user_b bigint not null references users(id) on delete cascade,
  created_at timestamp not null default now(),
  constraint uq_pair unique (user_a, user_b)
);

create table messages (
  id bigserial primary key,
  match_id bigint not null references matches(id) on delete cascade,
  sender_id bigint not null references users(id) on delete cascade,
  body text not null,
  created_at timestamp not null default now()
);

create index idx_messages_match_created on messages(match_id, created_at);
