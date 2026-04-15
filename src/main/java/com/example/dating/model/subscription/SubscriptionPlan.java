package com.example.dating.model.subscription;

import java.util.Locale;

/** YouMe subscription tier driving AI depth, quotas, and upgrade prompts. */
public enum SubscriptionPlan {
	FREE,
	PLUS,
	GOLD;

	public static SubscriptionPlan fromDb(String raw) {
		if (raw == null || raw.isBlank())
			return FREE;
		try {
			return SubscriptionPlan.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return FREE;
		}
	}

	/** Next tier for monetization CTAs (null if already top). */
	public SubscriptionPlan suggestedUpgrade() {
		return switch (this) {
			case FREE -> PLUS;
			case PLUS -> GOLD;
			case GOLD -> null;
		};
	}

	/** Ordinal rank for upgrade rules (FREE &lt; PLUS &lt; GOLD). */
	public int rank() {
		return ordinal();
	}

	public boolean isStrictlyLowerThan(SubscriptionPlan other) {
		return other != null && rank() < other.rank();
	}
}
