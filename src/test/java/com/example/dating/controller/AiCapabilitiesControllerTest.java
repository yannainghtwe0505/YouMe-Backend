package com.example.dating.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.config.AiProperties;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.service.AiCoachService;
import com.example.dating.service.AiCoachService.AiMeta;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = AiCapabilitiesController.class)
class AiCapabilitiesControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AiCoachService aiCoachService;

	@MockBean
	private AiProperties aiProperties;

	@Test
	void capabilities_exposesFlags() throws Exception {
		when(aiCoachService.meta()).thenReturn(new AiMeta(true));
		when(aiProperties.isRedisUsageEnabled()).thenReturn(false);
		mockMvc.perform(get("/ai/capabilities"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.llmConfigured").value(true))
				.andExpect(jsonPath("$.redisUsageEnabled").value(false));
	}

	@Test
	void plansMatrix_containsGoldFairUseCap() throws Exception {
		when(aiProperties.getGoldFairUseDailyCap()).thenReturn(500);
		when(aiProperties.quotasFor(any(SubscriptionPlan.class))).thenReturn(new AiProperties.TierQuotas(3, 1, 1, 0));
		when(aiProperties.effectiveDailyLimit(any(SubscriptionPlan.class), any(AiFeature.class))).thenReturn(3);
		when(aiProperties.isUnlimitedTierLimit(any(SubscriptionPlan.class), any(AiFeature.class))).thenReturn(false);
		mockMvc.perform(get("/ai/plans"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plans.goldFairUseDailyCap").value(500));
	}
}
