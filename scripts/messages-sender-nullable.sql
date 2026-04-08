-- Run in any SQL client as owner/superuser if you see:
--   null value in column "sender_id" of relation "messages" violates not-null constraint
-- (Flyway off locally: Hibernate may not always emit DROP NOT NULL for this column.)
--
--   (run against your app database, e.g. DatingAppDB)

alter table messages alter column sender_id drop not null;
