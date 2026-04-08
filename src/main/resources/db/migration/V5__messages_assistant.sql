-- Assistant / system messages: nullable sender, message kind.

alter table messages alter column sender_id drop not null;

alter table messages
  add column if not exists message_kind varchar(24) not null default 'user';
