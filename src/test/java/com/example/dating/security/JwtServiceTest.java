package com.example.dating.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.jsonwebtoken.JwtException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class JwtServiceTest {

	@Autowired
	private JwtService jwtService;

	@Test
	void generateThenValidate_roundTripsUserId() {
		String token = jwtService.generate(42L, "pat@example.com");
		assertEquals(42L, jwtService.validateAndUserId(token));
	}

	@Test
	void validateAndUserId_rejectsMalformedToken() {
		assertThrows(JwtException.class, () -> jwtService.validateAndUserId("not-a-jwt"));
	}
}
