package com.example.dating.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.SafetyCenterService;

@RestController
@RequestMapping("/me/safety")
public class SafetyController {
	private final SafetyCenterService safetyCenterService;

	public SafetyController(SafetyCenterService safetyCenterService) {
		this.safetyCenterService = safetyCenterService;
	}

	@GetMapping
	public ResponseEntity<?> getSafetySummary(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		return ResponseEntity.ok(safetyCenterService.summaryForUser(userId));
	}
}
