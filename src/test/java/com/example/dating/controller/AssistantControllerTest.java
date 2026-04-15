package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.dto.AiQuotaView;
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
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = AssistantController.class)
class AssistantControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AssistantGreetingService assistantGreetingService;

	@MockBean
	private AiCoachService aiCoachService;

	@MockBean
	private TieredAiUsageService tieredAiUsageService;

	@MockBean
	private SubscriptionPlanService subscriptionPlanService;

	@MockBean
	private MessageRepo messageRepo;

	@MockBean
	private MatchQueryService matchQueryService;

	@MockBean
	private BlockService blockService;

	@MockBean
	private ProfileRepo profileRepo;

	@Test
	void icebreaker_returns403_whenBlocked() throws Exception {
		when(matchQueryService.userParticipatesInMatch(1L, 7L)).thenReturn(false);
		mockMvc.perform(post("/matches/7/assistant/icebreaker").with(userId(1L))).andExpect(status().isForbidden());
	}

	@Test
	void icebreaker_returnsPayload() throws Exception {
		when(matchQueryService.userParticipatesInMatch(2L, 7L)).thenReturn(true);
		when(matchQueryService.peerUserIdForMatch(7L, 2L)).thenReturn(Optional.of(9L));
		when(blockService.eitherBlocked(2L, 9L)).thenReturn(false);
		when(assistantGreetingService.trySeedAssistantGreeting(7L, 2L)).thenReturn(true);
		ProfileEntity prof = new ProfileEntity();
		when(profileRepo.findById(2L)).thenReturn(Optional.of(prof));
		when(subscriptionPlanService.resolve(prof)).thenReturn(SubscriptionPlan.PLUS);
		when(tieredAiUsageService.status(2L, SubscriptionPlan.PLUS, AiFeature.MATCH_GREETING))
				.thenReturn(AiQuotaView.limited(1, 10, 9));
		mockMvc.perform(post("/matches/7/assistant/icebreaker").with(userId(2L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.created").value(true))
				.andExpect(jsonPath("$.subscriptionPlan").value("PLUS"))
				.andExpect(jsonPath("$.greetingQuota.remaining").value(9));
		verify(assistantGreetingService).trySeedAssistantGreeting(7L, 2L);
	}
}
