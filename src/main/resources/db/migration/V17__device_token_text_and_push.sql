-- Web Push subscriptions are JSON payloads larger than 512 chars.
ALTER TABLE user_device_tokens ALTER COLUMN token TYPE TEXT;
