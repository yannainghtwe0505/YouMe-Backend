-- Discovery preferences (viewer) and lifestyle traits (self / matching).

alter table profiles
  add column if not exists discovery_settings jsonb;

alter table profiles
  add column if not exists lifestyle jsonb;
