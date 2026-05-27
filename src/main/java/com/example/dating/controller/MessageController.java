package com.example.dating.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

import com.example.dating.model.entity.MessageEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.MatchReadStateRepo;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;
import com.example.dating.service.PresenceService;
import com.example.dating.service.PushNotificationService;
import com.example.dating.service.RealtimeMessageBroadcaster;
import com.example.dating.web.MessageViews;

import jakarta.validation.constraints.NotBlank;

@RestController
@Validated
@RequestMapping("/matches/{matchId}/messages")
public class MessageController {
	private static final int MAX_MESSAGE_LENGTH = 2000;
	private final MessageRepo repo;
	private final MatchQueryService matchQueryService;
	private final BlockService blockService;
	private final RealtimeMessageBroadcaster realtimeMessageBroadcaster;
	private final PresenceService presenceService;
	private final MatchReadStateRepo matchReadStateRepo;
	private final ProfileRepo profileRepo;
	private final PushNotificationService pushNotificationService;

	public MessageController(MessageRepo repo, MatchQueryService matchQueryService, BlockService blockService,
			RealtimeMessageBroadcaster realtimeMessageBroadcaster, PresenceService presenceService,
			MatchReadStateRepo matchReadStateRepo, ProfileRepo profileRepo,
			PushNotificationService pushNotificationService) {
		this.repo = repo;
		this.matchQueryService = matchQueryService;
		this.blockService = blockService;
		this.realtimeMessageBroadcaster = realtimeMessageBroadcaster;
		this.presenceService = presenceService;
		this.matchReadStateRepo = matchReadStateRepo;
		this.profileRepo = profileRepo;
		this.pushNotificationService = pushNotificationService;
	}

	public static class SendReq {
		@NotBlank
		public String body;
	}

	@GetMapping
	public ResponseEntity<?> list(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId,
			@RequestParam(name = "page", defaultValue = "0") int page) {
		Long userId = Long.valueOf(me.getUsername());
		if (!matchQueryService.userParticipatesInMatch(userId, matchId))
			return ResponseEntity.status(403).build();
		if (matchQueryService.peerUserIdForMatch(matchId, userId)
				.map(peer -> blockService.eitherBlocked(userId, peer))
				.orElse(true)) {
			return ResponseEntity.status(403).build();
		}
		var slice = repo.findByMatchIdOrderByCreatedAtAsc(matchId, PageRequest.of(page, 50));
		presenceService.touch(userId);
		Instant peerLastReadAt = peerLastReadAt(matchId, userId).orElse(null);
		List<Map<String, Object>> content = slice.stream()
				.map(m -> MessageViews.toRow(m, userId, peerLastReadAt))
				.collect(Collectors.toList());
		return ResponseEntity.ok(Map.of(
				"content", content,
				"number", page,
				"size", slice.size()));
	}

	@PostMapping
	public ResponseEntity<?> send(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId,
			@RequestBody SendReq req) {
		Long userId = Long.valueOf(me.getUsername());
		if (!matchQueryService.userParticipatesInMatch(userId, matchId))
			return ResponseEntity.status(403).build();
		if (matchQueryService.peerUserIdForMatch(matchId, userId)
				.map(peer -> blockService.eitherBlocked(userId, peer))
				.orElse(true)) {
			return ResponseEntity.status(403).build();
		}
		String body = req == null || req.body == null ? "" : req.body.trim();
		if (body.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Message body is required."));
		}
		if (body.length() > MAX_MESSAGE_LENGTH) {
			return ResponseEntity.badRequest().body(Map.of("error", "Message body exceeds 2000 characters."));
		}
		MessageEntity m = new MessageEntity();
		m.setMatchId(matchId);
		m.setSenderId(userId);
		m.setBody(body);
		m.setMessageKind(MessageEntity.KIND_USER);
		m = repo.save(m);
		presenceService.touch(userId);
		realtimeMessageBroadcaster.broadcastNewChatMessage(matchId, m);
		matchQueryService.peerUserIdForMatch(matchId, userId).ifPresent(peerId -> {
			String senderName = profileRepo.findById(userId).map(ProfileEntity::getDisplayName).orElse(null);
			pushNotificationService.notifyNewMessage(peerId, matchId, senderName, body);
		});
		return ResponseEntity.ok(Map.of("id", m.getId()));
	}

	private Optional<Instant> peerLastReadAt(Long matchId, Long userId) {
		return matchQueryService.peerUserIdForMatch(matchId, userId)
				.flatMap(peer -> matchReadStateRepo.findByUserIdAndMatchId(peer, matchId))
				.map(rs -> rs.getLastReadAt());
	}
}
