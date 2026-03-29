package com.example.dating.service;


import com.example.dating.model.entity.LikeEntity;
import com.example.dating.model.entity.MatchEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.MatchRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LikeService {
	private final LikeRepo likeRepo;
	private final MatchRepo matchRepo;

	public LikeService(LikeRepo likeRepo, MatchRepo matchRepo) {
		this.likeRepo = likeRepo;
		this.matchRepo = matchRepo;
	}

	public List<Map<String, Object>> getLikesForUser(Long userId) {
		return likeRepo.findAll().stream()
			.filter(like -> like.getFromUser().equals(userId))
			.map(like -> {
				Long otherUserId = like.getToUser();
				boolean matched = likeRepo.existsByFromUserAndToUser(otherUserId, userId);
				Long matchId = null;
				if (matched) {
					Long a = Math.min(userId, otherUserId), b = Math.max(userId, otherUserId);
					matchId = matchRepo.findByUserAAndUserB(a, b).map(MatchEntity::getId).orElse(null);
				}
				Map<String, Object> likeData = new HashMap<>();
				likeData.put("id", like.getId());
				likeData.put("toUserId", otherUserId);
				likeData.put("toUserName", "User " + otherUserId);
				likeData.put("matched", matched);
				likeData.put("matchId", matchId);
				likeData.put("createdAt", like.getCreatedAt());
				return likeData;
			})
			.collect(Collectors.toList());
	}

	@Transactional
	public boolean likeAndMaybeMatch(Long from, Long to) {
		if (from.equals(to) || likeRepo.existsByFromUserAndToUser(from, to))
			return false;
		LikeEntity le = new LikeEntity();
		le.setFromUser(from);
		le.setToUser(to);
		likeRepo.save(le);
		if (likeRepo.existsByFromUserAndToUser(to, from)) {
			Long a = Math.min(from, to), b = Math.max(from, to);
			matchRepo.findByUserAAndUserB(a, b).orElseGet(() -> {
				MatchEntity m = new MatchEntity();
				m.setUserA(a);
				m.setUserB(b);
				return matchRepo.save(m);
			});
			return true;
		}
		return false;
	}
}
