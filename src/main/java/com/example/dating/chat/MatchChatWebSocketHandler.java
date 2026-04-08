package com.example.dating.chat;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MatchChatWebSocketHandler extends TextWebSocketHandler {
	private final ChatSessionRegistry registry;

	public MatchChatWebSocketHandler(ChatSessionRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		registry.register(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		registry.remove(session);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		/* Client is push-only; ignore or use for ping health later. */
	}
}
