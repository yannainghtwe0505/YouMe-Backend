-- Daily AI coach usage (shared pool: reply ideas + profile tips). Day boundary uses app.ai.usage-timezone.
create table if not exists ai_coach_daily_usage (
  user_id bigint not null references users(id) on delete cascade,
  usage_date date not null,
  usage_count int not null default 0,
  primary key (user_id, usage_date)
);

create index if not exists idx_ai_coach_usage_date on ai_coach_daily_usage (usage_date);

alter table users add column if not exists locale varchar(12);

update users set locale = 'en' where locale is null;
