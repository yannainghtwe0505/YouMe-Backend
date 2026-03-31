package com.example.dating.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.PassService;

@RestController
@RequestMapping("/dislikes")
public class DislikeController {
	private final PassService passService;

	public DislikeController(PassService passService) {
		this.passService = passService;
	}

	@PostMapping("/{toUserId}")
	public ResponseEntity<Void> pass(@AuthenticationPrincipal User me, @PathVariable("toUserId") Long toUserId) {
		passService.recordPass(Long.valueOf(me.getUsername()), toUserId);
		return ResponseEntity.ok().build();
	}
}
