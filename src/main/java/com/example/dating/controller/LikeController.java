package com.example.dating.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import com.example.dating.service.LikeService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/likes")
public class LikeController {
	private final LikeService svc;

	public LikeController(LikeService svc) {
		this.svc = svc;
	}

	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> getOutboundLikes(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		List<Map<String, Object>> likes = svc.getLikesForUser(userId);
		return ResponseEntity.ok(likes);
	}

	/**
	 * People who liked you (subscription-gated). Free: {@code likes_count}, {@code locked}, {@code placeholders}
	 * only — no liker identities. Plus/Gold: {@code likes} with full rows.
	 */
	@GetMapping("/inbound")
	public ResponseEntity<Map<String, Object>> getInboundLikes(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		return ResponseEntity.ok(svc.getInboundLikesPayloadForViewer(userId));
	}

	@PostMapping("/{toUserId}")
	public ResponseEntity<?> like(@AuthenticationPrincipal User me, @PathVariable("toUserId") Long toUserId) {
		var out = svc.likeAndMaybeMatch(Long.valueOf(me.getUsername()), toUserId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("matched", out.matched());
		body.put("matchId", out.matchId());
		return ResponseEntity.ok(body);
	}
}
