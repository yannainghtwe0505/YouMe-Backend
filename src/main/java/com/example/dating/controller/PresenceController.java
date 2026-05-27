package com.example.dating.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.PresenceService;

@RestController
@RequestMapping("/presence")
public class PresenceController {
	private final PresenceService presenceService;

	public PresenceController(PresenceService presenceService) {
		this.presenceService = presenceService;
	}

	@PostMapping("/ping")
	public ResponseEntity<?> ping(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		presenceService.touch(userId);
		return ResponseEntity.ok(Map.of("ok", true));
	}
}
