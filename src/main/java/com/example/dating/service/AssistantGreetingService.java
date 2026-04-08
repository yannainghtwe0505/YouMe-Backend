package com.example.dating.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.MessageEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;

@Service
public class AssistantGreetingService {
	private final MessageRepo messageRepo;
	private final MatchRepo matchRepo;
	private final ProfileRepo profileRepo;
	private final AiCoachService aiCoachService;
	private final RealtimeMessageBroadcaster realtimeMessageBroadcaster;

	public AssistantGreetingService(MessageRepo messageRepo, MatchRepo matchRepo, ProfileRepo profileRepo,
			AiCoachService aiCoachService, RealtimeMessageBroadcaster realtimeMessageBroadcaster) {
		this.messageRepo = messageRepo;
		this.matchRepo = matchRepo;
		this.profileRepo = profileRepo;
		this.aiCoachService = aiCoachService;
		this.realtimeMessageBroadcaster = realtimeMessageBroadcaster;
	}

	/**
	 * Idempotent: adds a single assistant welcome line when the match has no messages yet.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean trySeedAssistantGreeting(Long matchId) {
		if (messageRepo.countByMatchId(matchId) > 0)
			return false;
		MatchEntity match = matchRepo.findById(matchId).orElse(null);
		if (match == null)
			return false;
		String nameA = displayName(match.getUserA());
		String nameB = displayName(match.getUserB());
		String bioA = profileRepo.findById(match.getUserA()).map(ProfileEntity::getBio).orElse(null);
		String bioB = profileRepo.findById(match.getUserB()).map(ProfileEntity::getBio).orElse(null);
		String body = aiCoachService.generateMatchGreeting(nameA, nameB,
				bioA == null ? "" : bioA, bioB == null ? "" : bioB);
		MessageEntity m = new MessageEntity();
		m.setMatchId(matchId);
		m.setSenderId(null);
		m.setBody(body);
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
