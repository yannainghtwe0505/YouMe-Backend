package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.MessageEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.AiCoachService.AiMeta;
import com.example.dating.service.AiCoachService.MatchGreetingOutcome;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantGreetingServiceTest {

	@Mock
	private MessageRepo messageRepo;
	@Mock
	private MatchRepo matchRepo;
	@Mock
	private ProfileRepo profileRepo;
	@Mock
	private AiCoachService aiCoachService;
	@Mock
	private TieredAiUsageService tieredAiUsageService;
	@Mock
	private SubscriptionPlanService subscriptionPlanService;
	@Mock
	private RealtimeMessageBroadcaster realtimeMessageBroadcaster;

	@InjectMocks
	private AssistantGreetingService assistantGreetingService;

	@Test
	void trySeedAssistantGreeting_returnsFalse_whenMessagesAlreadyExist() {
		when(messageRepo.countByMatchId(9L)).thenReturn(3L);
		assertFalse(assistantGreetingService.trySeedAssistantGreeting(9L, 1L));
		verify(matchRepo, never()).findById(any());
	}

	@Test
	void trySeedAssistantGreeting_returnsFalse_whenMatchMissing() {
		when(messageRepo.countByMatchId(9L)).thenReturn(0L);
		when(matchRepo.findById(9L)).thenReturn(java.util.Optional.empty());
		assertFalse(assistantGreetingService.trySeedAssistantGreeting(9L, 1L));
		verify(messageRepo, never()).save(any());
	}

	@Test
	void trySeedAssistantGreeting_skipsQuota_whenLlmNotConfigured() {
		long matchId = 100L;
		long actorId = 50L;
		when(messageRepo.countByMatchId(matchId)).thenReturn(0L);
		MatchEntity match = new MatchEntity();
		match.setId(matchId);
		match.setUserA(10L);
		match.setUserB(20L);
		when(matchRepo.findById(matchId)).thenReturn(java.util.Optional.of(match));
		when(profileRepo.findById(10L)).thenReturn(java.util.Optional.of(profile("Ann")));
		when(profileRepo.findById(20L)).thenReturn(java.util.Optional.of(profile("Bob")));
		when(profileRepo.findById(actorId)).thenReturn(java.util.Optional.of(profile("Ann")));
		when(subscriptionPlanService.resolve(any(ProfileEntity.class))).thenReturn(SubscriptionPlan.FREE);
		when(aiCoachService.meta()).thenReturn(new AiMeta(false));
		when(aiCoachService.generateMatchGreetingOutcome(eq("Ann"), eq("Bob"), eq(""), eq(""), eq(SubscriptionPlan.FREE)))
				.thenReturn(new MatchGreetingOutcome("Welcome both!", false));
		when(messageRepo.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		assertTrue(assistantGreetingService.trySeedAssistantGreeting(matchId, actorId));
		verify(tieredAiUsageService, never()).assertCanUse(anyLong(), any(), any());
		verify(tieredAiUsageService, never()).recordUse(anyLong(), any());
		verify(realtimeMessageBroadcaster).broadcastNewChatMessage(eq(matchId), any(MessageEntity.class));
	}

	private static ProfileEntity profile(String name) {
		ProfileEntity p = new ProfileEntity();
		p.setDisplayName(name);
		return p;
	}
}