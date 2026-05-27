package com.example.dating.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserReportEntity;
import com.example.dating.repository.BlockRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserReportRepo;

@Service
public class SafetyCenterService {
	private final BlockRepo blockRepo;
	private final UserReportRepo userReportRepo;
	private final ProfileRepo profileRepo;
	private final ProfileAvatarService profileAvatarService;

	public SafetyCenterService(BlockRepo blockRepo, UserReportRepo userReportRepo, ProfileRepo profileRepo,
			ProfileAvatarService profileAvatarService) {
		this.blockRepo = blockRepo;
		this.userReportRepo = userReportRepo;
		this.profileRepo = profileRepo;
		this.profileAvatarService = profileAvatarService;
	}

	public Map<String, Object> summaryForUser(Long userId) {
		List<Map<String, Object>> blocks = listBlockedUsers(userId);
		List<Map<String, Object>> reports = listMyReports(userId);
		return Map.of(
				"blockedUsers", blocks,
				"myReports", reports);
	}

	public List<Map<String, Object>> listBlockedUsers(Long blockerId) {
		List<Long> blockedIds = blockRepo.findBlockedIdsByBlocker(blockerId);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Long blockedId : blockedIds) {
			Map<String, Object> row = new HashMap<>();
			row.put("userId", blockedId);
			ProfileEntity profile = profileRepo.findById(blockedId).orElse(null);
			if (profile != null) {
				row.put("displayName", profile.getDisplayName());
				row.put("avatar", profileAvatarService.resolveAvatarUrl(blockedId, profile));
			} else {
				row.put("displayName", "User " + blockedId);
				row.put("avatar", null);
			}
			out.add(row);
		}
		return out;
	}

	public List<Map<String, Object>> listMyReports(Long reporterId) {
		return userReportRepo.findByReporterIdOrderByCreatedAtDesc(reporterId).stream()
				.map(this::toReportRow)
				.toList();
	}

	private Map<String, Object> toReportRow(UserReportEntity r) {
		Map<String, Object> row = new HashMap<>();
		row.put("id", r.getId());
		row.put("targetUserId", r.getTargetUserId());
		row.put("matchId", r.getMatchId());
		row.put("reason", r.getReason());
		row.put("details", r.getDetails());
		row.put("status", r.getStatus());
		row.put("createdAt", r.getCreatedAt());
		profileRepo.findById(r.getTargetUserId()).ifPresent(p -> {
			row.put("targetDisplayName", p.getDisplayName());
		});
		return row;
	}
}
