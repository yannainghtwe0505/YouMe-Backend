package com.example.dating.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RegistrationProfilePatchTest {

	@Test
	void fieldsRoundTrip() {
		RegistrationProfilePatch p = new RegistrationProfilePatch();
		p.displayName = "Yuki";
		p.interests = List.of("music", "travel");
		p.acceptTos = true;

		assertEquals("Yuki", p.displayName);
		assertEquals(2, p.interests.size());
		assertTrue(p.acceptTos);
	}
}
