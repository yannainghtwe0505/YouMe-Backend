package com.example.dating.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiQuotaViewTest {

	@Test
	void factoryMethodsSetFairUseFlag() {
		AiQuotaView limited = AiQuotaView.limited(1, 10, 9);
		assertEquals(1, limited.usedToday());
		assertEquals(10, limited.dailyLimit());
		assertEquals(9, limited.remaining());
		assertFalse(limited.fairUseCap());

		AiQuotaView fair = AiQuotaView.fairUse(100, 500, 400);
		assertTrue(fair.fairUseCap());
	}
}
