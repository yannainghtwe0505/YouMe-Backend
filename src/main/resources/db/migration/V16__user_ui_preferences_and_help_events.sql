-- Product onboarding, tooltip dismissal, and help analytics (cross-device sync).
ALTER TABLE users ADD COLUMN IF NOT EXISTS ui_preferences JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE TABLE IF NOT EXISTS user_help_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_name VARCHAR(80) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_help_events_user_created
    ON user_help_events (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_help_events_name
    ON user_help_events (event_name);
