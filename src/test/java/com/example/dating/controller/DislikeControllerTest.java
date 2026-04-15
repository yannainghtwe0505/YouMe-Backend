package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.PassService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = DislikeController.class)
class DislikeControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PassService passService;

	@Test
	void postPass_returns200() throws Exception {
		mockMvc.perform(post("/dislikes/22").with(userId(7L))).andExpect(status().isOk());
		verify(passService).recordPass(7L, 22L);
	}
}
