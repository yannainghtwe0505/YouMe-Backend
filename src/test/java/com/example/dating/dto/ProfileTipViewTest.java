package com.example.dating.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProfileTipViewTest {

	@Test
	void accessorsExposeComponents() {
		ProfileTipView v = new ProfileTipView("Headline", "Longer body text");
		assertEquals("Headline", v.title());
		assertEquals("Longer body text", v.detail());
	}
}
