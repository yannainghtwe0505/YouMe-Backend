package com.example.dating.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.subscription.SubscriptionPlan;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AiPropertiesTest {

	@Autowired
	private AiProperties aiProperties;

	@Test
	void quotasForPlus_reflectsConfiguredTier() {
		var q = aiProperties.quotasFor(SubscriptionPlan.PLUS);
		assertTrue(q.getChatReplies() > 0);
		assertEquals(20, q.getChatReplies());
	}

	@Test
	void model_isNonBlank() {
		assertTrue(!aiProperties.getModel().isBlank());
	}
}
