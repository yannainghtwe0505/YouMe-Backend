-- Run as PostgreSQL superuser (e.g. postgres) if tables were created by another role
-- and you want user "DatingAppDB" to be able to run ALTER / Hibernate ddl-auto: update.
--
--   psql -U postgres -d DatingAppDB -f scripts/grant-schema-ownership.sql
--
-- Replace DatingAppDB with your app username if different.

ALTER TABLE IF EXISTS messages OWNER TO "DatingAppDB";
ALTER TABLE IF EXISTS matches OWNER TO "DatingAppDB";
ALTER TABLE IF EXISTS likes OWNER TO "DatingAppDB";
ALTER TABLE IF EXISTS passes OWNER TO "DatingAppDB";
ALTER TABLE IF EXISTS photos OWNER TO "DatingAppDB";
ALTER TABLE IF EXISTS profiles OWNER TO "DatingAppDB";
ALTER TABLE IF EXISTS users OWNER TO "DatingAppDB";
