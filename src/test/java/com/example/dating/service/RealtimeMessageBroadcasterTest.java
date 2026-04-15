package com.example.dating.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.chat.ChatSessionRegistry;
import com.example.dating.model.entity.MessageEntity;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RealtimeMessageBroadcasterTest {

	@Mock
	private ChatSessionRegistry chatSessionRegistry;

	@InjectMocks
	private RealtimeMessageBroadcaster broadcaster;

	@Test
	void broadcastNewChatMessage_delegatesToRegistry() {
		MessageEntity m = new MessageEntity();
		m.setId(77L);
		m.setBody("hello");
		broadcaster.broadcastNewChatMessage(500L, m);
		verify(chatSessionRegistry).broadcastNewChatMessage(500L, m);
	}
}