package com.example.dating.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.dto.AiQuotaView;
import com.example.dating.model.entity.MessageEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.AiCoachService;
import com.example.dating.service.AssistantGreetingService;
import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;
import com.example.dating.service.SubscriptionPlanService;
import com.example.dating.service.TieredAiUsageService;

@RestController
@RequestMapping("/matches/{matchId}/assistant")
public class AssistantController {
	private final AssistantGreetingService assistantGreetingService;
	private final AiCoachService aiCoachService;
	private final TieredAiUsageService tieredAiUsageService;
	private final SubscriptionPlanService subscriptionPlanService;
	private final MessageRepo messageRepo;
	private final MatchQueryService matchQueryService;
	private final BlockService blockService;
	private final ProfileRepo profileRepo;

	public AssistantController(AssistantGreetingService assistantGreetingService, AiCoachService aiCoachService,
			TieredAiUsageService tieredAiUsageService, SubscriptionPlanService subscriptionPlanService,
			MessageRepo messageRepo, MatchQueryService matchQueryService, BlockService blockService,
			ProfileRepo profileRepo) {
		this.assistantGreetingService = assistantGreetingService;
		this.aiCoachService = aiCoachService;
		this.tieredAiUsageService = tieredAiUsageService;
		this.subscriptionPlanService = subscriptionPlanService;
		this.messageRepo = messageRepo;
		this.matchQueryService = matchQueryService;
		this.blockService = blockService;
		this.profileRepo = profileRepo;
	}

	public static class ReplyIdeasReq {
		public String tone;
	}

	private boolean blockedOrForbidden(Long userId, Long matchId) {
		if (!matchQueryService.userParticipatesInMatch(userId, matchId))
			return true;
		return matchQueryService.peerUserIdForMatch(matchId, userId)
				.map(peer -> blockService.eitherBlocked(userId, peer))
				.orElse(true);
	}

	@PostMapping("/icebreaker")
	public ResponseEntity<?> icebreaker(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId) {
		Long userId = Long.valueOf(me.getUsername());
		if (blockedOrForbidden(userId, matchId))
			return ResponseEntity.status(403).build();
		boolean created = assistantGreetingService.trySeedAssistantGreeting(matchId, userId);
		ProfileEntity profile = profileRepo.findById(userId).orElse(null);
		SubscriptionPlan plan = subscriptionPlanService.resolve(profile);
		AiQuotaView gq = tieredAiUsageService.status(userId, plan, AiFeature.MATCH_GREETING);
		return ResponseEntity.ok(Map.of(
				"created", created,
				"subscriptionPlan", plan.name(),
				"greetingQuota", Map.of(
						"usedToday", gq.usedToday(),
						"dailyLimit", gq.dailyLimit(),
						"remaining", gq.remaining(),
						"fairUseCap", gq.fairUseCap())));
	}

	@PostMapping("/reply-ideas")
	public ResponseEntity<?> replyIdeas(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId,
			@RequestBody(required = false) ReplyIdeasReq req) {
		Long userId = Long.valueOf(me.getUsername());
		if (blockedOrForbidden(userId, matchId))
			return ResponseEntity.status(403).build();
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
		boolean llm = aiCoachService.meta().llmConfigured();
		ProfileEntity profile = profileRepo.findById(userId).orElse(null);
		SubscriptionPlan plan = subscriptionPlanService.resolve(profile);
		if (llm) {
			tieredAiUsageService.assertCanUse(userId, plan, AiFeature.CHAT_REPLY);
		}
		var ideas = aiCoachService.replyIdeas(peerName, peerLines, tone, plan);
		if (llm) {
			tieredAiUsageService.recordUse(userId, AiFeature.CHAT_REPLY);
		}
		AiQuotaView q = tieredAiUsageService.status(userId, plan, AiFeature.CHAT_REPLY);
		return ResponseEntity.ok(Map.of(
				"ideas", ideas,
				"llmConfigured", llm,
				"subscriptionPlan", plan.name(),
				"aiQuota", Map.of(
						"usedToday", q.usedToday(),
						"dailyLimit", q.dailyLimit(),
						"remaining", q.remaining(),
						"fairUseCap", q.fairUseCap()),
				"aiEntitlements", tieredAiUsageService.entitlementsMap(userId, plan)));
	}

	/**
	 * Compatibility insight; FREE returns a minimal template without consuming quota. PLUS/GOLD use LLM when configured.
	 */
	@PostMapping("/match-insight")
	public ResponseEntity<?> matchInsight(@AuthenticationPrincipal User me, @PathVariable("matchId") Long matchId) {
		Long userId = Long.valueOf(me.getUsername());
		if (blockedOrForbidden(userId, matchId))
			return ResponseEntity.status(403).build();
		ProfileEntity viewer = profileRepo.findById(userId).orElseThrow();
		Long peerId = matchQueryService.peerUserIdForMatch(matchId, userId).orElseThrow();
		ProfileEntity peer = profileRepo.findById(peerId).orElseThrow();
		SubscriptionPlan plan = subscriptionPlanService.resolve(viewer);
		boolean llm = aiCoachService.meta().llmConfigured();
		if (plan != SubscriptionPlan.FREE && llm) {
			tieredAiUsageService.assertCanUse(userId, plan, AiFeature.MATCH_INSIGHT);
		}
		Map<String, Object> insight = new HashMap<>(
				aiCoachService.matchCompatibilityInsight(viewer, peer, plan));
		if (plan != SubscriptionPlan.FREE && llm && "llm".equals(String.valueOf(insight.get("source")))) {
			tieredAiUsageService.recordUse(userId, AiFeature.MATCH_INSIGHT);
		}
		AiQuotaView q = tieredAiUsageService.status(userId, plan, AiFeature.MATCH_INSIGHT);
		return ResponseEntity.ok(Map.of(
				"insight", insight,
				"llmConfigured", llm,
				"subscriptionPlan", plan.name(),
				"aiQuota", Map.of(
						"usedToday", q.usedToday(),
						"dailyLimit", q.dailyLimit(),
						"remaining", q.remaining(),
						"fairUseCap", q.fairUseCap())));
	}
}
