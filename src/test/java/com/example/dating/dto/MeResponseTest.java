package com.example.dating.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.dating.model.entity.PendingRegistrationEntity;

class MeResponseTest {

	@Test
	void fromPending_trimsDisplayNameFromDraft() {
		PendingRegistrationEntity row = new PendingRegistrationEntity();
		row.setEmail("pending@example.com");
		row.setOnboardingStep("PROFILE");
		Map<String, Object> draft = Map.of("displayName", "  Rin  ");

		MeResponse r = MeResponse.fromPending(row, draft);

		assertEquals("Rin", r.name());
		assertEquals("pending@example.com", r.email());
		assertFalse(r.registrationComplete());
	}
}
