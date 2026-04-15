package com.example.dating.model.subscription;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SubscriptionPlanTest {

	@Test
	void valuesNonEmpty() {
		assertTrue(SubscriptionPlan.values().length > 0);
	}
}
