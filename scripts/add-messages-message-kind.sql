-- Run as PostgreSQL superuser or table owner when the app user lacks ownership and Hibernate logs:
--   ERROR: must be owner of table messages / profiles
--
-- psql -U postgres -d DatingAppDB -f scripts/add-messages-message-kind.sql

-- V5 assistant messages
ALTER TABLE messages ADD COLUMN IF NOT EXISTS message_kind varchar(24) NOT NULL DEFAULT 'user';
ALTER TABLE messages ALTER COLUMN sender_id DROP NOT NULL;

-- V4 discovery (profiles)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS discovery_settings jsonb;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS lifestyle jsonb;
