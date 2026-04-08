package com.example.dating.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.dating.model.entity.MessageEntity;

public final class MessageViews {
	private MessageViews() {
	}

	public static Map<String, Object> toRow(MessageEntity m, Long currentUserId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", m.getId());
		row.put("body", m.getBody());
		row.put("createdAt", m.getCreatedAt());
		row.put("senderId", m.getSenderId());
		row.put("messageKind", m.getMessageKind());
		boolean assistant = MessageEntity.KIND_ASSISTANT.equals(m.getMessageKind());
		row.put("isAssistant", assistant);
		row.put("isFromCurrentUser", !assistant && m.getSenderId() != null && m.getSenderId().equals(currentUserId));
		return row;
	}

	/** WS payload: clients reload thread or merge using senderId / messageKind. */
	public static Map<String, Object> toBroadcastPayload(MessageEntity m) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", m.getId());
		row.put("body", m.getBody());
		row.put("createdAt", m.getCreatedAt());
		row.put("senderId", m.getSenderId());
		row.put("messageKind", m.getMessageKind());
		row.put("isAssistant", MessageEntity.KIND_ASSISTANT.equals(m.getMessageKind()));
		return row;
	}
}
