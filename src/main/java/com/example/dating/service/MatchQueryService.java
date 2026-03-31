package com.example.dating.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.ProfileRepo;

@Service
public class MatchQueryService {
	private final MatchRepo matchRepo;
	private final ProfileRepo profileRepo;

	public MatchQueryService(MatchRepo matchRepo, ProfileRepo profileRepo) {
		this.matchRepo = matchRepo;
		this.profileRepo = profileRepo;
	}

	public List<Map<String, Object>> listMatchesForUser(Long userId) {
		return matchRepo.findAllByUserInvolved(userId).stream()
				.map(m -> toSummary(m, userId))
				.collect(Collectors.toList());
	}

	private Map<String, Object> toSummary(MatchEntity m, Long me) {
		Long peerId = m.getUserA().equals(me) ? m.getUserB() : m.getUserA();
		Map<String, Object> row = new HashMap<>();
		row.put("matchId", m.getId());
		row.put("peerUserId", peerId);
		ProfileEntity peer = profileRepo.findById(peerId).orElse(null);
		if (peer != null) {
			row.put("peerName", peer.getDisplayName() != null ? peer.getDisplayName() : "User " + peerId);
			row.put("peerAvatar", peer.getPhotoUrl());
		} else {
			row.put("peerName", "User " + peerId);
			row.put("peerAvatar", null);
		}
		return row;
	}

	public boolean userParticipatesInMatch(Long userId, Long matchId) {
		return matchRepo.findById(matchId)
				.map(m -> m.getUserA().equals(userId) || m.getUserB().equals(userId))
				.orElse(false);
	}
}
