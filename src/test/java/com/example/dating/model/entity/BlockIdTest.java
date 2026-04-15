package com.example.dating.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class BlockIdTest {

	@Test
	void equalsAndHashCode_useBothParts() {
		BlockId a = new BlockId(1L, 2L);
		BlockId b = new BlockId(1L, 2L);
		BlockId c = new BlockId(2L, 1L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}
}
