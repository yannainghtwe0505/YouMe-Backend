package com.example.dating.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;
import com.example.dating.service.MatchReadStateService;

@RestController
@RequestMapping("/matches")
public class MatchController {
	private final MatchQueryService matchQueryService;
	private final MatchReadStateService matchReadStateService;
	private final BlockService blockService;

	public MatchController(MatchQueryService matchQueryService, MatchReadStateService matchReadStateService,
			BlockService blockService) {
		this.matchQueryService = matchQueryService;
		this.matchReadStateService = matchReadStateService;
		this.blockService = blockService;
	}

	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> list(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		return ResponseEntity.ok(matchQueryService.listMatchesForUser(userId));
	}

	@GetMapping("/unread-total")
	public ResponseEntity<Map<String, Long>> unreadTotal(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		return ResponseEntity.ok(Map.of("total", matchQueryService.totalUnreadForUser(userId)));
	}

	@PostMapping("/{matchId}/read")
	public ResponseEntity<?> markRead(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId) {
		Long userId = Long.valueOf(me.getUsername());
		if (!matchQueryService.userParticipatesInMatch(userId, matchId)) {
			return ResponseEntity.status(403).build();
		}
		if (matchQueryService.peerUserIdForMatch(matchId, userId)
				.map(peer -> blockService.eitherBlocked(userId, peer))
				.orElse(true)) {
			return ResponseEntity.status(403).build();
		}
		matchReadStateService.markRead(userId, matchId);
		return ResponseEntity.ok(Map.of("ok", true));
	}

	@DeleteMapping("/{matchId}")
	public ResponseEntity<?> deleteMatch(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId) {
		return matchQueryService.deleteMatchIfParticipant(Long.valueOf(me.getUsername()), matchId)
				? ResponseEntity.noContent().build()
				: ResponseEntity.status(404).build();
	}
}
