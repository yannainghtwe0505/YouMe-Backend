package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.FeedService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = FeedController.class)
class FeedControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FeedService feedService;

	@Test
	void getFeed_returnsPayloadFromService() throws Exception {
		List<Map<String, Object>> cards = List.of(Map.of("userId", 12L, "displayName", "Rin"));
		when(feedService.feedForUser(3L)).thenReturn(cards);
		mockMvc.perform(get("/feed").with(userId(3L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].displayName").value("Rin"));
		verify(feedService).feedForUser(3L);
	}
}
