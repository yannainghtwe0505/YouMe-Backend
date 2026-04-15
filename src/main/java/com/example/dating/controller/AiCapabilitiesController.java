package com.example.dating.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.config.AiProperties;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.service.AiCoachService;

@RestController
@RequestMapping("/ai")
public class AiCapabilitiesController {
	private final AiCoachService aiCoachService;
	private final AiProperties aiProperties;

	public AiCapabilitiesController(AiCoachService aiCoachService, AiProperties aiProperties) {
		this.aiCoachService = aiCoachService;
		this.aiProperties = aiProperties;
	}

	/** Public: whether the server has model credentials (no user data). */
	@GetMapping("/capabilities")
	public Map<String, Object> capabilities() {
		return Map.of(
				"llmConfigured", aiCoachService.meta().llmConfigured(),
				"redisUsageEnabled", aiProperties.isRedisUsageEnabled());
	}

	/** Public matrix of daily limits per plan (for paywall UX). */
	@GetMapping("/plans")
	public Map<String, Object> plansMatrix() {
		Map<String, Object> plans = new LinkedHashMap<>();
		for (SubscriptionPlan p : SubscriptionPlan.values()) {
			Map<String, Object> features = new LinkedHashMap<>();
			for (AiFeature f : AiFeature.values()) {
				int raw = aiProperties.quotasFor(p).limit(f);
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("rawLimit", raw);
				row.put("effectiveDailyCap", aiProperties.effectiveDailyLimit(p, f));
				row.put("unlimitedTier", aiProperties.isUnlimitedTierLimit(p, f));
				features.put(f.apiKey(), row);
			}
			plans.put(p.name(), Map.of("features", features));
		}
		plans.put("goldFairUseDailyCap", aiProperties.getGoldFairUseDailyCap());
		return Map.of("plans", plans);
	}
}
