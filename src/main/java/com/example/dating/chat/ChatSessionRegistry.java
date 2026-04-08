package com.example.dating.chat;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.MessageEntity;
import com.example.dating.repository.MatchRepo;
import com.example.dating.web.MessageViews;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ChatSessionRegistry {
	private final MatchRepo matchRepo;
	private final ObjectMapper objectMapper;
	private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> byUser = new ConcurrentHashMap<>();

	public ChatSessionRegistry(MatchRepo matchRepo, ObjectMapper objectMapper) {
		this.matchRepo = matchRepo;
		this.objectMapper = objectMapper;
	}

	public void register(WebSocketSession session) {
		Object uid = session.getAttributes().get("userId");
		if (!(uid instanceof Long userId))
			return;
		byUser.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
	}

	public void remove(WebSocketSession session) {
		Object uid = session.getAttributes().get("userId");
		if (!(uid instanceof Long userId))
			return;
		var set = byUser.get(userId);
		if (set != null) {
			set.remove(session);
			if (set.isEmpty())
				byUser.remove(userId, set);
		}
	}

	public void sendToUsers(Set<Long> userIds, String json) {
		for (Long userId : userIds) {
			var set = byUser.get(userId);
			if (set == null || set.isEmpty())
				continue;
			for (WebSocketSession s : set) {
				if (!s.isOpen())
					continue;
				try {
					s.sendMessage(new TextMessage(json));
				} catch (IOException ignored) {
				}
			}
		}
	}

	/** WS notify both match participants; clients expect {@code type: "chat"} and {@code matchId}. */
	public void broadcastNewChatMessage(Long matchId, MessageEntity m) {
		if (matchId == null || m == null) {
			return;
		}
		MatchEntity match = matchRepo.findById(matchId).orElse(null);
		if (match == null) {
			return;
		}
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("type", "chat");
			payload.put("matchId", matchId);
			payload.put("message", MessageViews.toBroadcastPayload(m));
			String json = objectMapper.writeValueAsString(payload);
			sendToUsers(Set.of(match.getUserA(), match.getUserB()), json);
		} catch (Exception ignored) {
		}
	}
}
