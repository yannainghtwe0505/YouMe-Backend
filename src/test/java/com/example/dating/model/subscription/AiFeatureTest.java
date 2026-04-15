package com.example.dating.model.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AiFeatureTest {

	@Test
	void apiKey_mapsUnderscoresToHyphens() {
		assertEquals("chat-reply", AiFeature.CHAT_REPLY.apiKey());
		assertEquals("profile-ai", AiFeature.PROFILE_AI.apiKey());
	}

	@Test
	void valueOf_roundTrips() {
		assertEquals(AiFeature.MATCH_INSIGHT, AiFeature.valueOf("MATCH_INSIGHT"));
	}
}
