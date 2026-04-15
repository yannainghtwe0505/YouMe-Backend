package com.example.dating.model.subscription;

import java.util.Locale;

/** Where the paid subscription is billed. FREE users use {@link #NONE}. */
public enum BillingProvider {
	NONE,
	STRIPE,
	APPLE,
	GOOGLE;

	public static BillingProvider fromDb(String raw) {
		if (raw == null || raw.isBlank())
			return NONE;
		try {
			return BillingProvider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return NONE;
		}
	}
}
