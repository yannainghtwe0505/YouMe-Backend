package com.example.dating.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.HelpAnalyticsService;
import com.example.dating.service.HelpAssistantService;
import com.example.dating.service.UiPreferencesService;

@RestController
@RequestMapping("/me/help")
public class HelpController {
	private final UiPreferencesService uiPreferences;
	private final HelpAnalyticsService analytics;
	private final HelpAssistantService assistant;

	public HelpController(UiPreferencesService uiPreferences, HelpAnalyticsService analytics,
			HelpAssistantService assistant) {
		this.uiPreferences = uiPreferences;
		this.analytics = analytics;
		this.assistant = assistant;
	}

	@GetMapping("/preferences")
	public ResponseEntity<?> getPreferences(@AuthenticationPrincipal User me) {
		long userId = Long.parseLong(me.getUsername());
		return ResponseEntity.ok(uiPreferences.getPreferences(userId));
	}

	@PatchMapping("/preferences")
	public ResponseEntity<?> patchPreferences(@AuthenticationPrincipal User me,
			@RequestBody Map<String, Object> body) {
		long userId = Long.parseLong(me.getUsername());
		return ResponseEntity.ok(uiPreferences.patchPreferences(userId, body));
	}

	@PostMapping("/events")
	public ResponseEntity<?> recordEvents(@AuthenticationPrincipal User me,
			@RequestBody EventsBody body) {
		long userId = Long.parseLong(me.getUsername());
		int n = analytics.recordBatch(userId, body == null ? List.of() : body.events);
		return ResponseEntity.ok(Map.of("recorded", n));
	}

	@PostMapping("/ask")
	public ResponseEntity<?> ask(@AuthenticationPrincipal User me, @RequestBody AskBody body) {
		long userId = Long.parseLong(me.getUsername());
		String q = body == null ? "" : body.question;
		String ctx = body == null ? "general" : body.context;
		String locale = body == null ? "en" : body.locale;
		var answer = assistant.ask(userId, q, ctx, locale);
		return ResponseEntity.ok(Map.of(
				"answer", answer.answer(),
				"fromLlm", answer.fromLlm()));
	}

	public static class EventsBody {
		public List<Map<String, Object>> events;
	}

	public static class AskBody {
		public String question;
		public String context;
		public String locale;
	}
}
