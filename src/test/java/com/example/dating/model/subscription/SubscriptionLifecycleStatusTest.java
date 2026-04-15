package com.example.dating.model.subscription;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SubscriptionLifecycleStatusTest {

	@Test
	void valuesNonEmpty() {
		assertTrue(SubscriptionLifecycleStatus.values().length > 0);
	}
}
