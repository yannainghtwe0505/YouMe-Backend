package com.example.dating.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.NotificationPreferenceService;

@RestController
@RequestMapping("/me/notifications")
public class NotificationController {
	private final NotificationPreferenceService notificationPreferenceService;

	public NotificationController(NotificationPreferenceService notificationPreferenceService) {
		this.notificationPreferenceService = notificationPreferenceService;
	}

	public static class DeviceTokenReq {
		public String token;
		public String platform;
		public String locale;
		public Boolean enabled;
	}

	public static class PushPreferenceReq {
		public Boolean enabled;
	}

	@GetMapping("/preferences")
	public ResponseEntity<?> getPreferences(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		return ResponseEntity.ok(notificationPreferenceService.summary(userId));
	}

	@PutMapping("/preferences")
	public ResponseEntity<?> setPreferences(@AuthenticationPrincipal User me, @RequestBody PushPreferenceReq req) {
		if (req == null || req.enabled == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "enabled is required"));
		}
		Long userId = Long.valueOf(me.getUsername());
		notificationPreferenceService.setEnabledForUser(userId, req.enabled.booleanValue());
		return ResponseEntity.ok(notificationPreferenceService.summary(userId));
	}

	@PutMapping("/device-token")
	public ResponseEntity<?> registerDeviceToken(@AuthenticationPrincipal User me, @RequestBody DeviceTokenReq req) {
		if (req == null || req.token == null || req.token.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "token is required"));
		}
		Long userId = Long.valueOf(me.getUsername());
		try {
			notificationPreferenceService.registerOrUpdateToken(
					userId,
					req.token,
					req.platform,
					req.locale,
					req.enabled == null || req.enabled.booleanValue());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
		return ResponseEntity.ok(notificationPreferenceService.summary(userId));
	}

	@DeleteMapping("/device-token")
	public ResponseEntity<?> removeDeviceToken(@AuthenticationPrincipal User me, @RequestBody DeviceTokenReq req) {
		if (req == null || req.token == null || req.token.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "token is required"));
		}
		Long userId = Long.valueOf(me.getUsername());
		boolean removed;
		try {
			removed = notificationPreferenceService.removeToken(userId, req.token);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
		if (!removed) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(notificationPreferenceService.summary(userId));
	}
}
