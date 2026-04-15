package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;
import com.example.dating.service.MatchReadStateService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = MatchController.class)
class MatchControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private MatchQueryService matchQueryService;

	@MockBean
	private MatchReadStateService matchReadStateService;

	@MockBean
	private BlockService blockService;

	@Test
	void listMatches_ok() throws Exception {
		when(matchQueryService.listMatchesForUser(1L)).thenReturn(List.of(Map.of("matchId", 10L)));
		mockMvc.perform(get("/matches").with(userId(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].matchId").value(10));
	}

	@Test
	void unreadTotal_ok() throws Exception {
		when(matchQueryService.totalUnreadForUser(2L)).thenReturn(7L);
		mockMvc.perform(get("/matches/unread-total").with(userId(2L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(7));
	}

	@Test
	void markRead_returns403_whenNotParticipant() throws Exception {
		when(matchQueryService.userParticipatesInMatch(3L, 99L)).thenReturn(false);
		mockMvc.perform(post("/matches/99/read").with(userId(3L))).andExpect(status().isForbidden());
	}

	@Test
	void markRead_ok_whenAllowed() throws Exception {
		when(matchQueryService.userParticipatesInMatch(3L, 99L)).thenReturn(true);
		when(matchQueryService.peerUserIdForMatch(99L, 3L)).thenReturn(Optional.of(40L));
		when(blockService.eitherBlocked(3L, 40L)).thenReturn(false);
		mockMvc.perform(post("/matches/99/read").with(userId(3L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true));
		verify(matchReadStateService).markRead(3L, 99L);
	}

	@Test
	void deleteMatch_returns204() throws Exception {
		when(matchQueryService.deleteMatchIfParticipant(5L, 12L)).thenReturn(true);
		mockMvc.perform(delete("/matches/12").with(userId(5L))).andExpect(status().isNoContent());
	}

	@Test
	void deleteMatch_returns404() throws Exception {
		when(matchQueryService.deleteMatchIfParticipant(5L, 12L)).thenReturn(false);
		mockMvc.perform(delete("/matches/12").with(userId(5L))).andExpect(status().isNotFound());
	}
}
