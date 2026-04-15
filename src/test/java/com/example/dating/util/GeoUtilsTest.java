package com.example.dating.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

	@Test
	void haversineTokyoOsakaApproximateKm() {
		double km = GeoUtils.haversineKm(35.68, 139.76, 34.69, 135.50);
		assertEquals(403, km, 25);
	}
}
