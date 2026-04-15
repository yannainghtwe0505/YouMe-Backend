package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.config.AiProperties;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.service.AiCoachService.AiMeta;
import com.example.dating.service.AiCoachService.MatchGreetingOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiCoachServiceTest {

	@Mock
	private OpenAiClient openAi;

	@Mock
	private AiProperties aiProperties;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void meta_false_whenDisabledOrMissingKey() {
		when(aiProperties.isEnabled()).thenReturn(false);
		when(aiProperties.hasApiKey()).thenReturn(true);
		AiCoachService svc = new AiCoachService(openAi, aiProperties, objectMapper);
		AiMeta m = svc.meta();
		assertFalse(m.llmConfigured());
	}

	@Test
	void meta_true_whenEnabledWithKey() {
		when(aiProperties.isEnabled()).thenReturn(true);
		when(aiProperties.hasApiKey()).thenReturn(true);
		AiCoachService svc = new AiCoachService(openAi, aiProperties, objectMapper);
		assertTrue(svc.meta().llmConfigured());
	}

	@Test
	void generateMatchGreetingOutcome_fallsBackToTemplate_whenLlmReturnsNull() throws Exception {
		when(aiProperties.isEnabled()).thenReturn(true);
		when(aiProperties.hasApiKey()).thenReturn(true);
		when(openAi.chatCompletion(anyString(), anyString(), anyInt(), anyDouble())).thenReturn(null);
		AiCoachService svc = new AiCoachService(openAi, aiProperties, objectMapper);
		MatchGreetingOutcome o = svc.generateMatchGreetingOutcome("Ann", "Ben", "", "", SubscriptionPlan.FREE);
		assertFalse(o.fromLlm());
		assertTrue(o.text().contains("Ann") || o.text().contains("You two"));
	}
}