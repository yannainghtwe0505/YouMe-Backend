package com.example.dating.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.model.entity.MessageEntity;
import com.example.dating.repository.MessageRepo;
import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/matches/{matchId}/messages")
public class MessageController {
	private final MessageRepo repo;
	private final MatchQueryService matchQueryService;
	private final BlockService blockService;

	public MessageController(MessageRepo repo, MatchQueryService matchQueryService, BlockService blockService) {
		this.repo = repo;
		this.matchQueryService = matchQueryService;
		this.blockService = blockService;
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
		List<Map<String, Object>> content = slice.stream()
				.map(m -> toRow(m, userId))
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
		MessageEntity m = new MessageEntity();
		m.setMatchId(matchId);
		m.setSenderId(userId);
		m.setBody(req.body);
		m = repo.save(m);
		return ResponseEntity.ok(Map.of("id", m.getId()));
	}

	private static Map<String, Object> toRow(MessageEntity m, Long currentUserId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", m.getId());
		row.put("body", m.getBody());
		row.put("createdAt", m.getCreatedAt());
		row.put("isFromCurrentUser", m.getSenderId().equals(currentUserId));
		return row;
	}
}
