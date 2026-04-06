package com.example.dating.service;


import com.example.dating.model.entity.LikeEntity;
import com.example.dating.model.entity.MatchEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.PassRepo;
import com.example.dating.repository.ProfileRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
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
	private final PassRepo passRepo;
	private final MatchReadStateService matchReadStateService;
	private final BlockService blockService;

	public LikeService(LikeRepo likeRepo, MatchRepo matchRepo, ProfileRepo profileRepo, PassRepo passRepo,
			MatchReadStateService matchReadStateService, BlockService blockService) {
		this.likeRepo = likeRepo;
		this.matchRepo = matchRepo;
		this.profileRepo = profileRepo;
		this.passRepo = passRepo;
		this.matchReadStateService = matchReadStateService;
		this.blockService = blockService;
	}

	public List<Map<String, Object>> getLikesForUser(Long userId) {
		return likeRepo.findAll().stream()
			.filter(like -> like.getFromUser().equals(userId))
			.filter(like -> !blockService.eitherBlocked(userId, like.getToUser()))
			.map(like -> {
				Long otherUserId = like.getToUser();
				boolean matched = likeRepo.existsByFromUserAndToUser(otherUserId, userId);
				Long matchId = null;
				if (matched) {
					Long a = Math.min(userId, otherUserId), b = Math.max(userId, otherUserId);
					matchId = matchRepo.findByUserAAndUserB(a, b).map(MatchEntity::getId).orElse(null);
				}
				var profile = profileRepo.findById(otherUserId);
				String name = profile
						.map(p -> p.getDisplayName() != null ? p.getDisplayName() : "Member")
						.orElse("Member");
				String avatar = profile.map(p -> p.getPhotoUrl()).orElse(null);
				Map<String, Object> likeData = new HashMap<>();
				likeData.put("id", like.getId());
				likeData.put("toUserId", otherUserId);
				likeData.put("toUserName", name);
				likeData.put("toUserAvatar", avatar);
				likeData.put("superLike", like.isSuperLike());
				likeData.put("matched", matched);
				likeData.put("matchId", matchId);
				likeData.put("createdAt", like.getCreatedAt());
				return likeData;
			})
			.sorted(Comparator
					.<Map<String, Object>>comparingInt(m -> Boolean.TRUE.equals(m.get("matched")) ? 0 : 1)
					.thenComparing(m -> (Instant) m.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder())))
			.collect(Collectors.toList());
	}

	/**
	 * People who liked the current user but are not a mutual match yet - same idea as "Likes you" / beeline
	 * on Tinder, Bumble, etc. Excludes blocked users, people you already liked back (match), and people you passed.
	 */
	public List<Map<String, Object>> getInboundLikesForUser(Long userId) {
		return likeRepo.findByToUser(userId).stream()
				.filter(like -> !blockService.eitherBlocked(userId, like.getFromUser()))
				.filter(like -> !likeRepo.existsByFromUserAndToUser(userId, like.getFromUser()))
				.filter(like -> !passRepo.existsByFromUserAndToUser(userId, like.getFromUser()))
				.map(like -> {
					Long fromId = like.getFromUser();
					var profile = profileRepo.findById(fromId);
					String name = profile
							.map(p -> p.getDisplayName() != null ? p.getDisplayName() : "Member")
							.orElse("Member");
					String avatar = profile.map(p -> p.getPhotoUrl()).orElse(null);
					Map<String, Object> row = new HashMap<>();
					row.put("id", like.getId());
					row.put("fromUserId", fromId);
					row.put("fromUserName", name);
					row.put("fromUserAvatar", avatar);
					row.put("superLike", like.isSuperLike());
					row.put("createdAt", like.getCreatedAt());
					return row;
				})
				.sorted(Comparator.comparing(m -> (Instant) m.get("createdAt"),
						Comparator.nullsLast(Comparator.reverseOrder())))
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
		if (from.equals(to) || blockService.eitherBlocked(from, to) || likeRepo.existsByFromUserAndToUser(from, to))
			return LikeOutcome.noMatch();
		LikeEntity le = new LikeEntity();
		le.setFromUser(from);
		le.setToUser(to);
		le.setSuperLike(superLike);
		likeRepo.save(le);
		if (likeRepo.existsByFromUserAndToUser(to, from)) {
			Long a = Math.min(from, to), b = Math.max(from, to);
			return matchRepo.findByUserAAndUserB(a, b)
					.map(m -> LikeOutcome.matched(m.getId()))
					.orElseGet(() -> {
						MatchEntity m = new MatchEntity();
						m.setUserA(a);
						m.setUserB(b);
						MatchEntity saved = matchRepo.save(m);
						matchReadStateService.seedNewMatch(saved.getId(), a, b);
						return LikeOutcome.matched(saved.getId());
					});
		}
		return LikeOutcome.noMatch();
	}
}
