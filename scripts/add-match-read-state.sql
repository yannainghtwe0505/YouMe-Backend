-- Run as DB owner if Flyway is disabled (matches existing project pattern).
create table if not exists match_read_state (
  user_id bigint not null references users(id) on delete cascade,
  match_id bigint not null references matches(id) on delete cascade,
  last_read_at timestamptz not null,
  primary key (user_id, match_id)
);

create index if not exists idx_match_read_state_user on match_read_state(user_id);

insert into match_read_state (user_id, match_id, last_read_at)
select user_a, id, now() from matches
on conflict do nothing;

insert into match_read_state (user_id, match_id, last_read_at)
select user_b, id, now() from matches
on conflict do nothing;
