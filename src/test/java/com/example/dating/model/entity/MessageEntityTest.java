package com.example.dating.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MessageEntityTest {

	@Test
	void prePersist_defaultsBlankKindToUser() {
		MessageEntity m = new MessageEntity();
		m.setMessageKind("   ");
		m.pre();
		assertEquals(MessageEntity.KIND_USER, m.getMessageKind());
		assertNotNull(m.getCreatedAt());
	}
}
