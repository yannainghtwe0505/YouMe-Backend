package com.example.dating.service;


import com.example.dating.model.entity.LikeEntity;
import com.example.dating.model.entity.MatchEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.ProfileRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LikeService {

	public record LikeOutcome(boolean matched, Long matchId) {
		public static LikeOutcome noMatch() {
			return new LikeOutcome(false, null);
		}

		public static LikeOutcome matched(Long id) {
			return new LikeOutcome(true, id);
		}
	}
	private final LikeRepo likeRepo;
	private final MatchRepo matchRepo;
	private final ProfileRepo profileRepo;

	public LikeService(LikeRepo likeRepo, MatchRepo matchRepo, ProfileRepo profileRepo) {
		this.likeRepo = likeRepo;
		this.matchRepo = matchRepo;
		this.profileRepo = profileRepo;
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
				likeData.put("toUserName", profileRepo.findById(otherUserId)
						.map(p -> p.getDisplayName() != null ? p.getDisplayName() : "User " + otherUserId)
						.orElse("User " + otherUserId));
				likeData.put("superLike", like.isSuperLike());
				likeData.put("matched", matched);
				likeData.put("matchId", matchId);
				likeData.put("createdAt", like.getCreatedAt());
				return likeData;
			})
			.collect(Collectors.toList());
	}

	@Transactional
	public LikeOutcome likeAndMaybeMatch(Long from, Long to) {
		return saveLikeAndMaybeMatch(from, to, false);
	}

	@Transactional
	public LikeOutcome superLikeAndMaybeMatch(Long from, Long to) {
		return saveLikeAndMaybeMatch(from, to, true);
	}

	private LikeOutcome saveLikeAndMaybeMatch(Long from, Long to, boolean superLike) {
		if (from.equals(to) || likeRepo.existsByFromUserAndToUser(from, to))
			return LikeOutcome.noMatch();
		LikeEntity le = new LikeEntity();
		le.setFromUser(from);
		le.setToUser(to);
		le.setSuperLike(superLike);
		likeRepo.save(le);
		if (likeRepo.existsByFromUserAndToUser(to, from)) {
			Long a = Math.min(from, to), b = Math.max(from, to);
			MatchEntity saved = matchRepo.findByUserAAndUserB(a, b).orElseGet(() -> {
				MatchEntity m = new MatchEntity();
				m.setUserA(a);
				m.setUserB(b);
				return matchRepo.save(m);
			});
			return LikeOutcome.matched(saved.getId());
		}
		return LikeOutcome.noMatch();
	}
}
