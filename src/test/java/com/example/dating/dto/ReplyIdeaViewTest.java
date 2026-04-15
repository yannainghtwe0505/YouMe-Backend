package com.example.dating.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReplyIdeaViewTest {

	@Test
	void accessorsExposeComponents() {
		ReplyIdeaView v = new ReplyIdeaView("Hello back", "playful");
		assertEquals("Hello back", v.text());
		assertEquals("playful", v.type());
	}
}
