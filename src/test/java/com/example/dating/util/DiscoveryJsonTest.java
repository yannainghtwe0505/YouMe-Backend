package com.example.dating.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DiscoveryJsonTest {

	@Test
	void normGenderMapsMaleSynonyms() {
		assertEquals("men", DiscoveryJson.normGender("Male"));
	}

	@Test
	void truthyHandlesBooleanAndString() {
		assertTrue(DiscoveryJson.truthy(true));
		assertTrue(DiscoveryJson.truthy("1"));
		assertFalse(DiscoveryJson.truthy(false));
	}

	@Test
	void intValueParsesString() {
		assertEquals(7, DiscoveryJson.intValue("7", 0));
	}

	@Test
	void nestedMapCoercesKeys() {
		Map<String, Object> parent = Map.of("k", Map.of("x", 1));
		assertEquals(1, DiscoveryJson.nestedMap(parent, "k").get("x"));
	}

	@Test
	void stringListFromList() {
		assertEquals(List.of("a", "b"), DiscoveryJson.stringList(List.of(" a ", "b")));
	}
}
