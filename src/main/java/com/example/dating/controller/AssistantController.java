package com.example.dating.controller;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.model.entity.MessageEntity;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.AiCoachService;
import com.example.dating.service.AssistantGreetingService;
import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;

@RestController
@RequestMapping("/matches/{matchId}/assistant")
public class AssistantController {
	private final AssistantGreetingService assistantGreetingService;
	private final AiCoachService aiCoachService;
	private final MessageRepo messageRepo;
	private final MatchQueryService matchQueryService;
	private final BlockService blockService;
	private final ProfileRepo profileRepo;

	public AssistantController(AssistantGreetingService assistantGreetingService, AiCoachService aiCoachService,
			MessageRepo messageRepo, MatchQueryService matchQueryService, BlockService blockService,
			ProfileRepo profileRepo) {
		this.assistantGreetingService = assistantGreetingService;
		this.aiCoachService = aiCoachService;
		this.messageRepo = messageRepo;
		this.matchQueryService = matchQueryService;
		this.blockService = blockService;
		this.profileRepo = profileRepo;
	}

	public static class ReplyIdeasReq {
		public String tone;
	}

	@PostMapping("/icebreaker")
	public ResponseEntity<?> icebreaker(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId) {
		Long userId = Long.valueOf(me.getUsername());
		if (!matchQueryService.userParticipatesInMatch(userId, matchId))
			return ResponseEntity.status(403).build();
		if (matchQueryService.peerUserIdForMatch(matchId, userId)
				.map(peer -> blockService.eitherBlocked(userId, peer))
				.orElse(true)) {
			return ResponseEntity.status(403).build();
		}
		boolean created = assistantGreetingService.trySeedAssistantGreeting(matchId);
		return ResponseEntity.ok(Map.of("created", created));
	}

	@PostMapping("/reply-ideas")
	public ResponseEntity<?> replyIdeas(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId,
			@RequestBody(required = false) ReplyIdeasReq req) {
		Long userId = Long.valueOf(me.getUsername());
		if (!matchQueryService.userParticipatesInMatch(userId, matchId))
			return ResponseEntity.status(403).build();
		if (matchQueryService.peerUserIdForMatch(matchId, userId)
				.map(peer -> blockService.eitherBlocked(userId, peer))
				.orElse(true)) {
			return ResponseEntity.status(403).build();
		}
		String peerName = matchQueryService.peerUserIdForMatch(matchId, userId)
				.flatMap(profileRepo::findById)
				.map(p -> p.getDisplayName() != null && !p.getDisplayName().isBlank() ? p.getDisplayName() : "Member")
				.orElse("Match");
		List<MessageEntity> thread = messageRepo.findByMatchIdOrderByCreatedAtAsc(matchId, PageRequest.of(0, 80));
		List<String> peerLines = new ArrayList<>();
		for (MessageEntity m : thread) {
			if (!MessageEntity.KIND_USER.equals(m.getMessageKind()))
				continue;
			if (m.getSenderId() == null || m.getSenderId().equals(userId))
				continue;
			if (m.getBody() != null && !m.getBody().isBlank())
				peerLines.add(m.getBody().trim());
		}
		if (peerLines.size() > 6)
			peerLines = peerLines.subList(peerLines.size() - 6, peerLines.size());
		String tone = req != null ? req.tone : null;
		List<String> ideas = aiCoachService.replyIdeas(peerName, peerLines, tone);
		return ResponseEntity.ok(Map.of("ideas", ideas, "llmConfigured", aiCoachService.meta().llmConfigured()));
	}
}
