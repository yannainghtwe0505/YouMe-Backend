ALTER TABLE profiles
	ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(64),
	ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(64);
