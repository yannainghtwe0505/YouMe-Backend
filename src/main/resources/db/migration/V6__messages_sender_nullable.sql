-- Assistant messages use null sender_id. Safe if already nullable (re-run / idempotent).
alter table messages alter column sender_id drop not null;
