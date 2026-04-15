package com.example.dating.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AppUrlsPropertiesTest {

	@Autowired
	private AppUrlsProperties appUrlsProperties;

	@Test
	void testProfileBindsPublicApiBaseUrl() {
		assertTrue(appUrlsProperties.getApiPublicBaseUrl().contains("api.test"));
	}
}
