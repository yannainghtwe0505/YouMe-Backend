package com.example.dating.chat;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.dating.repository.MatchRepo;

@ExtendWith(MockitoExtension.class)
class ChatSessionRegistryTest {

	@Mock
	private MatchRepo matchRepo;

	@Test
	void registerThenSendToUsers_deliversJsonToOpenSession() throws Exception {
		ChatSessionRegistry registry = new ChatSessionRegistry(matchRepo, new ObjectMapper());
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getAttributes()).thenReturn(Map.of("userId", 9L));
		when(session.isOpen()).thenReturn(true);

		registry.register(session);
		registry.sendToUsers(Set.of(9L), "{\"ping\":true}");

		verify(session).sendMessage(argThat(tm -> "{\"ping\":true}".equals(tm.getPayload())));
	}
}
