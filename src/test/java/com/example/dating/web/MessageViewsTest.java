package com.example.dating.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.dating.model.entity.MessageEntity;

class MessageViewsTest {

	@Test
	void toRowFlagsAssistantAndSender() {
		MessageEntity m = new MessageEntity();
		m.setId(9L);
		m.setBody("hi");
		m.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));
		m.setSenderId(2L);
		m.setMessageKind(MessageEntity.KIND_USER);
		var row = MessageViews.toRow(m, 2L);
		assertFalse((Boolean) row.get("isAssistant"));
		assertTrue((Boolean) row.get("isFromCurrentUser"));
		assertEquals(9L, row.get("id"));
	}

	@Test
	void toBroadcastPayloadMarksAssistant() {
		MessageEntity m = new MessageEntity();
		m.setId(1L);
		m.setBody("x");
		m.setCreatedAt(Instant.now());
		m.setSenderId(null);
		m.setMessageKind(MessageEntity.KIND_ASSISTANT);
		var row = MessageViews.toBroadcastPayload(m);
		assertTrue((Boolean) row.get("isAssistant"));
	}
}
