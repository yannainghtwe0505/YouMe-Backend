package com.example.dating.model.subscription;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BillingProviderTest {

	@Test
	void valuesNonEmpty() {
		assertTrue(BillingProvider.values().length > 0);
	}
}
