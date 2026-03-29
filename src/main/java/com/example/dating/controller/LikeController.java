package com.example.dating.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import com.example.dating.service.LikeService;

import java.util.Map;

@RestController
@RequestMapping("/likes")
public class LikeController {
	private final LikeService svc;

	public LikeController(LikeService svc) {
		this.svc = svc;
	}

	@PostMapping("/{toUserId}")
	public ResponseEntity<?> like(@AuthenticationPrincipal User me, @PathVariable("toUserId") Long toUserId) {
		boolean matched = svc.likeAndMaybeMatch(Long.valueOf(me.getUsername()), toUserId);
		return ResponseEntity.ok(Map.of("matched", matched));
	}
}
