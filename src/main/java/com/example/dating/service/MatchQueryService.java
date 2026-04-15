package com.example.dating.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.MatchReadStateRepo;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;

@Service
public class MatchQueryService {
	private static final int PREVIEW_LEN = 120;

	private final MatchRepo matchRepo;
	private final ProfileRepo profileRepo;
	private final MessageRepo messageRepo;
	private final MatchReadStateRepo readStateRepo;
	private final BlockService blockService;
	private final ProfileAvatarService profileAvatarService;

	public MatchQueryService(MatchRepo matchRepo, ProfileRepo profileRepo, MessageRepo messageRepo,
			MatchReadStateRepo readStateRepo, BlockService blockService, ProfileAvatarService profileAvatarService) {
		this.matchRepo = matchRepo;
		this.profileRepo = profileRepo;
		this.messageRepo = messageRepo;
		this.readStateRepo = readStateRepo;
		this.blockService = blockService;
		this.profileAvatarService = profileAvatarService;
	}

	public List<Map<String, Object>> listMatchesForUser(Long userId) {
		Set<Long> hiddenPeers = blockService.hiddenPeerIdsFor(userId);
		return matchRepo.findAllByUserInvolved(userId).stream()
				.map(m -> toSummary(m, userId))
				.filter(row -> !hiddenPeers.contains(row.get("peerUserId")))
				.collect(Collectors.toList());
	}

	public long totalUnreadForUser(Long userId) {
		Set<Long> hiddenPeers = blockService.hiddenPeerIdsFor(userId);
		long total = 0;
		for (MatchEntity m : matchRepo.findAllByUserInvolved(userId)) {
			Long peerId = m.getUserA().equals(userId) ? m.getUserB() : m.getUserA();
			if (hiddenPeers.contains(peerId))
				continue;
			Instant since = readStateRepo.findByUserIdAndMatchId(userId, m.getId())
					.map(rs -> rs.getLastReadAt())
					.orElse(Instant.EPOCH);
			total += messageRepo.countByMatchIdAndSenderIdNotAndCreatedAtAfter(m.getId(), userId, since);
		}
		return total;
	}

	public Optional<Long> peerUserIdForMatch(Long matchId, Long userId) {
		return matchRepo.findById(matchId)
				.filter(m -> m.getUserA().equals(userId) || m.getUserB().equals(userId))
				.map(m -> m.getUserA().equals(userId) ? m.getUserB() : m.getUserA());
	}

	private Map<String, Object> toSummary(MatchEntity m, Long me) {
		Long peerId = m.getUserA().equals(me) ? m.getUserB() : m.getUserA();
		Map<String, Object> row = new HashMap<>();
		row.put("matchId", m.getId());
		row.put("peerUserId", peerId);
		ProfileEntity peer = profileRepo.findById(peerId).orElse(null);
		if (peer != null) {
			row.put("peerName", peer.getDisplayName() != null ? peer.getDisplayName() : "User " + peerId);
			row.put("peerAvatar", profileAvatarService.resolveAvatarUrl(peerId, peer));
		} else {
			row.put("peerName", "User " + peerId);
			row.put("peerAvatar", null);
		}

		Instant lastRead = readStateRepo.findByUserIdAndMatchId(me, m.getId())
				.map(rs -> rs.getLastReadAt())
				.orElse(Instant.EPOCH);
		long unread = messageRepo.countByMatchIdAndSenderIdNotAndCreatedAtAfter(m.getId(), me, lastRead);
		row.put("unreadCount", unread);

		messageRepo.findFirstByMatchIdOrderByCreatedAtDesc(m.getId()).ifPresentOrElse(msg -> {
			row.put("lastMessageBody", truncate(msg.getBody(), PREVIEW_LEN));
			row.put("lastMessageAt", msg.getCreatedAt());
		}, () -> {
			row.put("lastMessageBody", null);
			row.put("lastMessageAt", null);
		});

		return row;
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		String t = s.strip();
		if (t.length() <= max) {
			return t;
		}
		return t.substring(0, max - 1) + "…";
	}

	public boolean userParticipatesInMatch(Long userId, Long matchId) {
		return matchRepo.findById(matchId)
				.map(m -> m.getUserA().equals(userId) || m.getUserB().equals(userId))
				.orElse(false);
	}

	@Transactional
	public boolean deleteMatchIfParticipant(Long userId, Long matchId) {
		return matchRepo.findById(matchId)
				.filter(m -> m.getUserA().equals(userId) || m.getUserB().equals(userId))
				.map(m -> {
					matchRepo.delete(m);
					return true;
				})
				.orElse(false);
	}
}
