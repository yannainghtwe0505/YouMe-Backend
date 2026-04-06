package com.example.dating.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.BlockService;

@RestController
@RequestMapping("/blocks")
public class BlockController {
	private final BlockService blockService;

	public BlockController(BlockService blockService) {
		this.blockService = blockService;
	}

	@PostMapping("/{userId}")
	public ResponseEntity<?> block(@AuthenticationPrincipal User me, @PathVariable("userId") Long userId) {
		Long blocker = Long.valueOf(me.getUsername());
		if (blocker.equals(userId)) {
			return ResponseEntity.badRequest().body(Map.of("error", "cannot block yourself"));
		}
		blockService.block(blocker, userId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<?> unblock(@AuthenticationPrincipal User me, @PathVariable("userId") Long userId) {
		Long blocker = Long.valueOf(me.getUsername());
		return blockService.unblock(blocker, userId) ? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}
}
