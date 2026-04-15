package com.example.dating.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.MessageEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;

@Service
public class AssistantGreetingService {
	private final MessageRepo messageRepo;
	private final MatchRepo matchRepo;
	private final ProfileRepo profileRepo;
	private final AiCoachService aiCoachService;
	private final TieredAiUsageService tieredAiUsageService;
	private final SubscriptionPlanService subscriptionPlanService;
	private final RealtimeMessageBroadcaster realtimeMessageBroadcaster;

	public AssistantGreetingService(MessageRepo messageRepo, MatchRepo matchRepo, ProfileRepo profileRepo,
			AiCoachService aiCoachService, TieredAiUsageService tieredAiUsageService,
			SubscriptionPlanService subscriptionPlanService, RealtimeMessageBroadcaster realtimeMessageBroadcaster) {
		this.messageRepo = messageRepo;
		this.matchRepo = matchRepo;
		this.profileRepo = profileRepo;
		this.aiCoachService = aiCoachService;
		this.tieredAiUsageService = tieredAiUsageService;
		this.subscriptionPlanService = subscriptionPlanService;
		this.realtimeMessageBroadcaster = realtimeMessageBroadcaster;
	}

	/**
	 * Idempotent: adds a single assistant welcome line when the match has no messages yet.
	 * {@code actingUserId} is charged for LLM greeting quota when the model is used.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean trySeedAssistantGreeting(Long matchId, Long actingUserId) {
		if (messageRepo.countByMatchId(matchId) > 0)
			return false;
		MatchEntity match = matchRepo.findById(matchId).orElse(null);
		if (match == null)
			return false;
		String nameA = displayName(match.getUserA());
		String nameB = displayName(match.getUserB());
		String bioA = profileRepo.findById(match.getUserA()).map(ProfileEntity::getBio).orElse(null);
		String bioB = profileRepo.findById(match.getUserB()).map(ProfileEntity::getBio).orElse(null);
		ProfileEntity actor = profileRepo.findById(actingUserId).orElse(null);
		var plan = subscriptionPlanService.resolve(actor);
		boolean llm = aiCoachService.meta().llmConfigured();
		if (llm) {
			tieredAiUsageService.assertCanUse(actingUserId, plan, AiFeature.MATCH_GREETING);
		}
		var outcome = aiCoachService.generateMatchGreetingOutcome(nameA, nameB,
				bioA == null ? "" : bioA, bioB == null ? "" : bioB, plan);
		if (llm && outcome.fromLlm()) {
			tieredAiUsageService.recordUse(actingUserId, AiFeature.MATCH_GREETING);
		}
		MessageEntity m = new MessageEntity();
		m.setMatchId(matchId);
		m.setSenderId(null);
		m.setBody(outcome.text());
		m.setMessageKind(MessageEntity.KIND_ASSISTANT);
		m = messageRepo.save(m);
		realtimeMessageBroadcaster.broadcastNewChatMessage(matchId, m);
		return true;
	}

	private String displayName(Long userId) {
		return profileRepo.findById(userId)
				.map(p -> p.getDisplayName() != null && !p.getDisplayName().isBlank()
						? p.getDisplayName()
						: "Member")
				.orElse("Member");
	}
}
