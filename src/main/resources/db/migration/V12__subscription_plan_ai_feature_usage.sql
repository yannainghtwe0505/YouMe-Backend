-- Tiered subscriptions + per-feature AI usage (also created by Hibernate ddl-auto when enabled).
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(20) NOT NULL DEFAULT 'FREE';

UPDATE profiles SET subscription_plan = 'PLUS' WHERE is_premium = true AND subscription_plan = 'FREE';

CREATE TABLE IF NOT EXISTS ai_feature_daily_usage (
	user_id BIGINT NOT NULL,
	usage_date DATE NOT NULL,
	feature VARCHAR(32) NOT NULL,
	usage_count INT NOT NULL DEFAULT 0,
	PRIMARY KEY (user_id, usage_date, feature)
);
