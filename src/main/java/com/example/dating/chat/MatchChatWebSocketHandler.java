package com.example.dating.chat;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.dating.service.BlockService;
import com.example.dating.service.MatchQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MatchChatWebSocketHandler extends TextWebSocketHandler {
	private final ChatSessionRegistry registry;
	private final ObjectMapper objectMapper;
	private final MatchQueryService matchQueryService;
	private final BlockService blockService;

	public MatchChatWebSocketHandler(ChatSessionRegistry registry, ObjectMapper objectMapper,
			MatchQueryService matchQueryService, BlockService blockService) {
		this.registry = registry;
		this.objectMapper = objectMapper;
		this.matchQueryService = matchQueryService;
		this.blockService = blockService;
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
		Object uid = session.getAttributes().get("userId");
		if (!(uid instanceof Long userId)) {
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
			String type = payload.get("type") instanceof String s ? s.trim() : "";
			if (!"typing".equals(type)) {
				return;
			}
			Long matchId = null;
			Object rawMatchId = payload.get("matchId");
			if (rawMatchId instanceof Number n) {
				matchId = n.longValue();
			} else if (rawMatchId instanceof String s && !s.isBlank()) {
				matchId = Long.valueOf(s);
			}
			if (matchId == null || !matchQueryService.userParticipatesInMatch(userId, matchId)) {
				return;
			}
			boolean blocked = matchQueryService.peerUserIdForMatch(matchId, userId)
					.map(peer -> blockService.eitherBlocked(userId, peer))
					.orElse(true);
			if (blocked) {
				return;
			}
			boolean isTyping = payload.get("isTyping") instanceof Boolean b ? b : false;
			registry.broadcastTyping(matchId, userId, isTyping);
		} catch (Exception ignored) {
		}
	}
}
