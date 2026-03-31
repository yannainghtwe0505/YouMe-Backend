package com.example.dating.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.MatchQueryService;

@RestController
@RequestMapping("/matches")
public class MatchController {
	private final MatchQueryService matchQueryService;

	public MatchController(MatchQueryService matchQueryService) {
		this.matchQueryService = matchQueryService;
	}

	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> list(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		return ResponseEntity.ok(matchQueryService.listMatchesForUser(userId));
	}
}
