CREATE TABLE IF NOT EXISTS user_subscription (
	user_id BIGINT NOT NULL PRIMARY KEY REFERENCES users (id),
	plan_tier VARCHAR(12) NOT NULL DEFAULT 'FREE',
	billing_provider VARCHAR(16) DEFAULT 'NONE',
	external_subscription_id VARCHAR(255),
	receipt_data TEXT,
	lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
	current_period_end TIMESTAMPTZ,
	cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_subscription_lifecycle ON user_subscription (lifecycle_status);
CREATE INDEX IF NOT EXISTS idx_user_subscription_provider ON user_subscription (billing_provider);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_subscription_external_unique ON user_subscription (external_subscription_id)
	WHERE external_subscription_id IS NOT NULL AND external_subscription_id <> '';

INSERT INTO user_subscription (
	user_id,
	plan_tier,
	billing_provider,
	external_subscription_id,
	receipt_data,
	lifecycle_status,
	current_period_end,
	cancel_at_period_end
)
SELECT
	p.user_id,
	COALESCE(NULLIF(TRIM(p.subscription_plan), ''), 'FREE'),
	CASE
		WHEN p.stripe_subscription_id IS NOT NULL AND TRIM(p.stripe_subscription_id) <> '' THEN 'STRIPE'
		ELSE 'NONE'
	END,
	NULLIF(TRIM(p.stripe_subscription_id), ''),
	NULL,
	CASE
		WHEN COALESCE(NULLIF(TRIM(p.subscription_plan), ''), 'FREE') = 'FREE'
			AND (p.stripe_subscription_id IS NULL OR TRIM(p.stripe_subscription_id) = '') THEN 'NONE'
		ELSE 'ACTIVE'
	END,
	NULL,
	FALSE
FROM profiles p
ON CONFLICT (user_id) DO NOTHING;
