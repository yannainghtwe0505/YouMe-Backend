package com.example.dating.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.UserReportService;

@RestController
@RequestMapping("/reports")
public class ReportController {
	private final UserReportService userReportService;

	public ReportController(UserReportService userReportService) {
		this.userReportService = userReportService;
	}

	public static class ReportUserReq {
		public String reason;
		public String details;
		public Long matchId;
	}

	@PostMapping("/users/{userId}")
	public ResponseEntity<?> reportUser(@AuthenticationPrincipal User me, @PathVariable("userId") Long targetUserId,
			@RequestBody(required = false) ReportUserReq req) {
		Long reporterId = Long.valueOf(me.getUsername());
		String reason = req == null ? null : req.reason;
		String details = req == null ? null : req.details;
		Long matchId = req == null ? null : req.matchId;
		var report = userReportService.create(reporterId, targetUserId, matchId, reason, details);
		return ResponseEntity.ok(Map.of(
				"id", report.getId(),
				"status", report.getStatus(),
				"createdAt", report.getCreatedAt()));
	}
}
