-- Fix: permission denied for table users (42501) on AuthController / UserRepo
--
-- Run as superuser or table owner, e.g. in pgAdmin: Query Tool on database DatingAppDB
-- Or from a shell where psql is installed:
--   psql -U postgres -d DatingAppDB -f scripts/grant-dating-app-privileges.sql
--
-- spring.datasource.username is DatingAppDB → PostgreSQL role is usually lowercase: datingappdb
-- Use the block below first (unquoted name). If your role was created with quotes, use the second block.

-- === Option A (most common): unquoted role → matches datingappdb ===
GRANT USAGE, CREATE ON SCHEMA public TO DatingAppDB;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO DatingAppDB;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO DatingAppDB;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO DatingAppDB;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO DatingAppDB;

-- === Option B: only if Option A errors with "role datingappdb does not exist" ===
-- GRANT USAGE, CREATE ON SCHEMA public TO "DatingAppDB";
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "DatingAppDB";
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO "DatingAppDB";
