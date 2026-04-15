package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.BlockService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = BlockController.class)
class BlockControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private BlockService blockService;

	@Test
	void postBlock_returns204_andInvokesService() throws Exception {
		mockMvc.perform(post("/blocks/5").with(userId(1L))).andExpect(status().isNoContent());
		verify(blockService).block(1L, 5L);
	}

	@Test
	void postBlock_returns400_whenSelfBlock() throws Exception {
		mockMvc.perform(post("/blocks/1").with(userId(1L)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("cannot block yourself"));
		verify(blockService, never()).block(anyLong(), anyLong());
	}

	@Test
	void deleteBlock_returns204_whenRemoved() throws Exception {
		when(blockService.unblock(2L, 9L)).thenReturn(true);
		mockMvc.perform(delete("/blocks/9").with(userId(2L))).andExpect(status().isNoContent());
		verify(blockService).unblock(2L, 9L);
	}

	@Test
	void deleteBlock_returns404_whenNotFound() throws Exception {
		when(blockService.unblock(2L, 9L)).thenReturn(false);
		mockMvc.perform(delete("/blocks/9").with(userId(2L))).andExpect(status().isNotFound());
	}
}
