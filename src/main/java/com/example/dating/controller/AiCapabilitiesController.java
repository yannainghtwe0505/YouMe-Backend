package com.example.dating.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.AiCoachService;

@RestController
@RequestMapping("/ai")
public class AiCapabilitiesController {
	private final AiCoachService aiCoachService;

	public AiCapabilitiesController(AiCoachService aiCoachService) {
		this.aiCoachService = aiCoachService;
	}

	/** Public: only reveals whether the server has AI configured (no user-specific data). */
	@GetMapping("/capabilities")
	public Map<String, Object> capabilities() {
		return Map.of("llmConfigured", aiCoachService.meta().llmConfigured());
	}
}
