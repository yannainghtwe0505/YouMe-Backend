package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.model.entity.MessageEntity;
import com.example.dating.repository.MessageRepo;
import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;
import com.example.dating.service.RealtimeMessageBroadcaster;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = MessageController.class)
class MessageControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private MessageRepo messageRepo;

	@MockBean
	private MatchQueryService matchQueryService;

	@MockBean
	private BlockService blockService;

	@MockBean
	private RealtimeMessageBroadcaster realtimeMessageBroadcaster;

	@Test
	void list_returns403_whenNotParticipant() throws Exception {
		when(matchQueryService.userParticipatesInMatch(1L, 50L)).thenReturn(false);
		mockMvc.perform(get("/matches/50/messages").with(userId(1L))).andExpect(status().isForbidden());
	}

	@Test
	void list_returnsPage() throws Exception {
		when(matchQueryService.userParticipatesInMatch(1L, 50L)).thenReturn(true);
		when(matchQueryService.peerUserIdForMatch(50L, 1L)).thenReturn(java.util.Optional.of(2L));
		when(blockService.eitherBlocked(1L, 2L)).thenReturn(false);
		MessageEntity m = new MessageEntity();
		m.setId(9L);
		m.setBody("hi");
		m.setSenderId(2L);
		m.setMessageKind(MessageEntity.KIND_USER);
		when(messageRepo.findByMatchIdOrderByCreatedAtAsc(50L, PageRequest.of(0, 50))).thenReturn(List.of(m));
		mockMvc.perform(get("/matches/50/messages").with(userId(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.number").value(0))
				.andExpect(jsonPath("$.content[0].body").value("hi"));
	}

	@Test
	void send_persistsAndBroadcasts() throws Exception {
		when(matchQueryService.userParticipatesInMatch(3L, 60L)).thenReturn(true);
		when(matchQueryService.peerUserIdForMatch(60L, 3L)).thenReturn(java.util.Optional.of(4L));
		when(blockService.eitherBlocked(3L, 4L)).thenReturn(false);
		when(messageRepo.save(any(MessageEntity.class))).thenAnswer(inv -> {
			MessageEntity e = inv.getArgument(0);
			e.setId(100L);
			return e;
		});
		mockMvc.perform(post("/matches/60/messages").with(userId(3L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"Hello there\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(100));
		verify(realtimeMessageBroadcaster).broadcastNewChatMessage(eq(60L), any(MessageEntity.class));
	}
}
