package com.example.dating.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.example.dating.dto.AiQuotaView;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;

class AiCoachQuotaExceededExceptionTest {

	@Test
	void singleArgConstructorSetsDefaults() {
		AiCoachQuotaExceededException ex = new AiCoachQuotaExceededException(AiQuotaView.limited(1, 3, 2));
		assertNotNull(ex.getMessage());
		assertEquals(AiFeature.CHAT_REPLY, ex.getFeature());
		assertEquals(SubscriptionPlan.FREE, ex.getCurrentPlan());
	}

	@Test
	void fullConstructorPreservesPlans() {
		AiCoachQuotaExceededException ex = new AiCoachQuotaExceededException(
				AiQuotaView.limited(0, 1, 1),
				AiFeature.PROFILE_AI,
				SubscriptionPlan.PLUS,
				SubscriptionPlan.GOLD);
		assertEquals(AiFeature.PROFILE_AI, ex.getFeature());
		assertEquals(SubscriptionPlan.PLUS, ex.getCurrentPlan());
		assertEquals(SubscriptionPlan.GOLD, ex.getSuggestedUpgrade());
	}
}
