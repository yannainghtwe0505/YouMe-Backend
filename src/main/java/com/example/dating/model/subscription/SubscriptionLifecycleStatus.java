package com.example.dating.model.subscription;

import java.util.Locale;

/**
 * Lifecycle for a user's subscription row. Separate from {@link SubscriptionPlan} (tier).
 */
public enum SubscriptionLifecycleStatus {
	/** No paid subscription row or organic free user. */
	NONE,
	/** Awaiting payment / receipt confirmation (short-lived). */
	PENDING,
	ACTIVE,
	CANCELED,
	EXPIRED;

	public static SubscriptionLifecycleStatus fromDb(String raw) {
		if (raw == null || raw.isBlank())
			return NONE;
		try {
			return SubscriptionLifecycleStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return NONE;
		}
	}
}
