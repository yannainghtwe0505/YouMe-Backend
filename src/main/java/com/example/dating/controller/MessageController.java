package com.example.dating.controller;

import java.util.Map;

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

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/matches/{matchId}/messages")
public class MessageController {
	private final MessageRepo repo;

	public MessageController(MessageRepo repo) {
		this.repo = repo;
	}

	public static class SendReq {
		@NotBlank
		public String body;
	}

	@GetMapping
	public ResponseEntity<?> list(@PathVariable("matchId") Long matchId,
			@RequestParam(name = "page", defaultValue = "0") int page) {
		return ResponseEntity.ok(repo.findByMatchIdOrderByCreatedAtAsc(matchId, PageRequest.of(page, 50)));
	}

	@PostMapping
	public ResponseEntity<?> send(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId,
			@RequestBody SendReq req) {
		MessageEntity m = new MessageEntity();
		m.setMatchId(matchId);
		m.setSenderId(Long.valueOf(me.getUsername()));
		m.setBody(req.body);
		m = repo.save(m);
		return ResponseEntity.ok(Map.of("id", m.getId()));
	}
}
