package com.example.dating.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokyoMunicipalitiesTest {

	@Test
	void shinjukuAllowedUnknownRejected() {
		assertTrue(TokyoMunicipalities.isAllowed("Shinjuku-ku"));
		assertFalse(TokyoMunicipalities.isAllowed("Osaka-shi"));
	}
}
