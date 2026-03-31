package com.example.dating.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.LikeService;

@RestController
@RequestMapping("/superlikes")
public class SuperlikeController {
	private final LikeService likeService;

	public SuperlikeController(LikeService likeService) {
		this.likeService = likeService;
	}

	@PostMapping("/{toUserId}")
	public ResponseEntity<?> superLike(@AuthenticationPrincipal User me, @PathVariable("toUserId") Long toUserId) {
		var out = likeService.superLikeAndMaybeMatch(Long.valueOf(me.getUsername()), toUserId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("matched", out.matched());
		body.put("matchId", out.matchId());
		return ResponseEntity.ok(body);
	}
}
