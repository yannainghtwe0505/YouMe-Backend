alter table users
	add column if not exists last_active_at timestamptz;

create table if not exists user_reports (
	id bigserial primary key,
	reporter_id bigint not null references users(id) on delete cascade,
	target_user_id bigint not null references users(id) on delete cascade,
	match_id bigint references matches(id) on delete set null,
	reason varchar(64) not null,
	details varchar(2000),
	status varchar(24) not null default 'OPEN',
	created_at timestamptz not null default now()
);

create index if not exists idx_user_reports_target_created
	on user_reports(target_user_id, created_at desc);
create index if not exists idx_user_reports_reporter_created
	on user_reports(reporter_id, created_at desc);

create table if not exists user_device_tokens (
	id bigserial primary key,
	user_id bigint not null references users(id) on delete cascade,
	token varchar(512) not null unique,
	platform varchar(24) not null,
	enabled boolean not null default true,
	locale varchar(12),
	created_at timestamptz not null default now(),
	last_seen_at timestamptz not null default now()
);

create index if not exists idx_user_device_tokens_user_enabled
	on user_device_tokens(user_id, enabled);
