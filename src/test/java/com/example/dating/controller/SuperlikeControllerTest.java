package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.LikeService;
import com.example.dating.service.LikeService.LikeOutcome;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = SuperlikeController.class)
class SuperlikeControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LikeService likeService;

	@Test
	void superLike_returnsOutcomeJson() throws Exception {
		when(likeService.superLikeAndMaybeMatch(8L, 9L)).thenReturn(LikeOutcome.noMatch());
		mockMvc.perform(post("/superlikes/9").with(userId(8L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matched").value(false));
		verify(likeService).superLikeAndMaybeMatch(8L, 9L);
	}
}
