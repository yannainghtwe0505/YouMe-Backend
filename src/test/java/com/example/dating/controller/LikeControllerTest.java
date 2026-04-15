package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.LikeService;
import com.example.dating.service.LikeService.LikeOutcome;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = LikeController.class)
class LikeControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LikeService likeService;

	@Test
	void getOutboundLikes_ok() throws Exception {
		when(likeService.getLikesForUser(4L)).thenReturn(List.of(Map.of("toUserId", 9L)));
		mockMvc.perform(get("/likes").with(userId(4L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].toUserId").value(9));
	}

	@Test
	void getInboundLikes_ok() throws Exception {
		when(likeService.getInboundLikesPayloadForViewer(4L)).thenReturn(Map.of("likes_count", 2L, "locked", true));
		mockMvc.perform(get("/likes/inbound").with(userId(4L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likes_count").value(2));
	}

	@Test
	void postLike_returnsMatchPayload() throws Exception {
		when(likeService.likeAndMaybeMatch(4L, 10L)).thenReturn(LikeOutcome.matched(500L));
		mockMvc.perform(post("/likes/10").with(userId(4L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matched").value(true))
				.andExpect(jsonPath("$.matchId").value(500));
		verify(likeService).likeAndMaybeMatch(4L, 10L);
	}
}
