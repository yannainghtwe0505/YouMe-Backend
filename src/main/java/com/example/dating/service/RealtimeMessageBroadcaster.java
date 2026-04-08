package com.example.dating.service;

import org.springframework.stereotype.Service;

import com.example.dating.chat.ChatSessionRegistry;
import com.example.dating.model.entity.MessageEntity;

/**
 * Facade for chat WebSocket push; implementation lives in {@link ChatSessionRegistry}.
 */
@Service
public class RealtimeMessageBroadcaster {
	private final ChatSessionRegistry chatSessionRegistry;

	public RealtimeMessageBroadcaster(ChatSessionRegistry chatSessionRegistry) {
		this.chatSessionRegistry = chatSessionRegistry;
	}

	public void broadcastNewChatMessage(Long matchId, MessageEntity m) {
		chatSessionRegistry.broadcastNewChatMessage(matchId, m);
	}
}
