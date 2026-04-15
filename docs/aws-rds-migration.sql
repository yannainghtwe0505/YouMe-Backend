-- =============================================================================
-- YouMe / dating-app — consolidated PostgreSQL schema for AWS RDS
-- =============================================================================
-- Generated from Flyway migrations V1–V14 and JPA entity mappings.
-- Intended for an EMPTY database (fresh RDS instance). Apply as superuser or
-- owner that can CREATE TABLE / INDEX.
--
-- Does NOT include: seed data, Flyway history table, or data backfills from
-- incremental migrations (use Flyway in app deploy for upgrades).
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. users
-- -----------------------------------------------------------------------------
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE,
  phone_e164 VARCHAR(32) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_login TIMESTAMPTZ,
  registration_complete BOOLEAN NOT NULL DEFAULT TRUE,
  onboarding_step VARCHAR(40),
  tos_accepted_at TIMESTAMPTZ,
  privacy_accepted_at TIMESTAMPTZ,
  locale VARCHAR(12),
  CONSTRAINT users_login_identifier CHECK (email IS NOT NULL OR phone_e164 IS NOT NULL)
);

CREATE UNIQUE INDEX uq_users_phone_e164 ON users (phone_e164) WHERE phone_e164 IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 2. pending_registrations (onboarding before user row exists)
-- -----------------------------------------------------------------------------
CREATE TABLE pending_registrations (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255),
  phone_e164 VARCHAR(32),
  channel VARCHAR(16) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  session_token VARCHAR(64) UNIQUE,
  session_expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  password_hash VARCHAR(255),
  onboarding_step VARCHAR(40),
  tos_accepted_at TIMESTAMPTZ,
  privacy_accepted_at TIMESTAMPTZ,
  profile_draft JSONB,
  CONSTRAINT pending_has_contact CHECK (email IS NOT NULL OR phone_e164 IS NOT NULL)
);

CREATE UNIQUE INDEX uq_pending_email ON pending_registrations (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uq_pending_phone ON pending_registrations (phone_e164) WHERE phone_e164 IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 3. profiles
-- -----------------------------------------------------------------------------
CREATE TABLE profiles (
  user_id BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
  display_name VARCHAR(100),
  bio VARCHAR(2000),
  gender VARCHAR(20),
  birthday DATE,
  interests JSONB,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  min_age INT,
  max_age INT,
  distance_km INT,
  city VARCHAR(255),
  education VARCHAR(200),
  occupation VARCHAR(200),
  hobbies VARCHAR(500),
  photo_url VARCHAR(1024),
  is_premium BOOLEAN NOT NULL DEFAULT FALSE,
  subscription_plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
  stripe_customer_id VARCHAR(64),
  stripe_subscription_id VARCHAR(64),
  discovery_settings JSONB,
  lifestyle JSONB
);

-- -----------------------------------------------------------------------------
-- 4. photos
-- -----------------------------------------------------------------------------
CREATE TABLE photos (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  s3_key VARCHAR(512) NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_photos_user_id ON photos (user_id);

-- -----------------------------------------------------------------------------
-- 5. likes
-- -----------------------------------------------------------------------------
CREATE TABLE likes (
  id BIGSERIAL PRIMARY KEY,
  from_user BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  to_user BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  super_like BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_likes_from_to UNIQUE (from_user, to_user)
);

CREATE INDEX idx_likes_to_user ON likes (to_user);
CREATE INDEX idx_likes_from_user ON likes (from_user);

-- -----------------------------------------------------------------------------
-- 6. passes (dislikes)
-- -----------------------------------------------------------------------------
CREATE TABLE passes (
  id BIGSERIAL PRIMARY KEY,
  from_user BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  to_user BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_passes_from_to UNIQUE (from_user, to_user)
);

CREATE INDEX idx_passes_to_user ON passes (to_user);

-- -----------------------------------------------------------------------------
-- 7. matches
-- -----------------------------------------------------------------------------
CREATE TABLE matches (
  id BIGSERIAL PRIMARY KEY,
  user_a BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  user_b BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_match_pair UNIQUE (user_a, user_b)
);

CREATE INDEX idx_matches_user_a ON matches (user_a);
CREATE INDEX idx_matches_user_b ON matches (user_b);

-- -----------------------------------------------------------------------------
-- 8. messages
-- -----------------------------------------------------------------------------
CREATE TABLE messages (
  id BIGSERIAL PRIMARY KEY,
  match_id BIGINT NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
  sender_id BIGINT REFERENCES users (id) ON DELETE CASCADE,
  body VARCHAR(2000) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  message_kind VARCHAR(24) NOT NULL DEFAULT 'user'
);

CREATE INDEX idx_messages_match_created ON messages (match_id, created_at);

-- -----------------------------------------------------------------------------
-- 9. match_read_state
-- -----------------------------------------------------------------------------
CREATE TABLE match_read_state (
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  match_id BIGINT NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
  last_read_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (user_id, match_id)
);

CREATE INDEX idx_match_read_state_user ON match_read_state (user_id);

-- -----------------------------------------------------------------------------
-- 10. blocks
-- -----------------------------------------------------------------------------
CREATE TABLE blocks (
  blocker_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  blocked_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (blocker_id, blocked_id),
  CONSTRAINT blocks_no_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX idx_blocks_blocked ON blocks (blocked_id);

-- -----------------------------------------------------------------------------
-- 11. ai_coach_daily_usage (legacy / shared counter table)
-- -----------------------------------------------------------------------------
CREATE TABLE ai_coach_daily_usage (
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  usage_date DATE NOT NULL,
  usage_count INT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, usage_date)
);

CREATE INDEX idx_ai_coach_usage_date ON ai_coach_daily_usage (usage_date);

-- -----------------------------------------------------------------------------
-- 12. ai_feature_daily_usage (per-feature quotas)
-- -----------------------------------------------------------------------------
CREATE TABLE ai_feature_daily_usage (
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  usage_date DATE NOT NULL,
  feature VARCHAR(32) NOT NULL,
  usage_count INT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, usage_date, feature)
);

-- -----------------------------------------------------------------------------
-- 13. user_subscription (billing state)
-- -----------------------------------------------------------------------------
CREATE TABLE user_subscription (
  user_id BIGINT NOT NULL PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
  plan_tier VARCHAR(12) NOT NULL DEFAULT 'FREE',
  billing_provider VARCHAR(16) DEFAULT 'NONE',
  external_subscription_id VARCHAR(255),
  receipt_data TEXT,
  lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
  current_period_end TIMESTAMPTZ,
  cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_subscription_lifecycle ON user_subscription (lifecycle_status);
CREATE INDEX idx_user_subscription_provider ON user_subscription (billing_provider);
CREATE UNIQUE INDEX idx_user_subscription_external_unique ON user_subscription (external_subscription_id)
  WHERE external_subscription_id IS NOT NULL AND external_subscription_id <> '';

COMMIT;

-- End of schema
