package com.example.dating.service;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.model.entity.UserReportEntity;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.UserReportRepo;

@Service
public class UserReportService {
	private static final Set<String> ALLOWED_REASONS = Set.of(
			"HARASSMENT",
			"SPAM",
			"SCAM",
			"INAPPROPRIATE_CONTENT",
			"UNDERAGE",
			"OTHER");

	private final UserReportRepo userReportRepo;
	private final MatchRepo matchRepo;

	public UserReportService(UserReportRepo userReportRepo, MatchRepo matchRepo) {
		this.userReportRepo = userReportRepo;
		this.matchRepo = matchRepo;
	}

	public UserReportEntity create(Long reporterId, Long targetUserId, Long matchId, String reason, String details) {
		if (reporterId.equals(targetUserId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot report yourself");
		}
		String normalizedReason = normalizeReason(reason);
		if (!ALLOWED_REASONS.contains(normalizedReason)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report reason");
		}
		if (matchId != null) {
			var match = matchRepo.findById(matchId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid matchId"));
			boolean participant = reporterId.equals(match.getUserA()) || reporterId.equals(match.getUserB());
			boolean targetIsPeer = targetUserId.equals(match.getUserA()) || targetUserId.equals(match.getUserB());
			if (!participant || !targetIsPeer) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report must reference your match");
			}
		}
		UserReportEntity report = new UserReportEntity();
		report.setReporterId(reporterId);
		report.setTargetUserId(targetUserId);
		report.setMatchId(matchId);
		report.setReason(normalizedReason);
		report.setDetails(normalizeDetails(details));
		return userReportRepo.save(report);
	}

	private static String normalizeReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return "OTHER";
		}
		return reason.trim().toUpperCase();
	}

	private static String normalizeDetails(String details) {
		if (details == null) {
			return null;
		}
		String text = details.trim();
		if (text.isEmpty()) {
			return null;
		}
		if (text.length() > 2000) {
			return text.substring(0, 2000);
		}
		return text;
	}
}
