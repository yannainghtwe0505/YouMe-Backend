package com.example.dating.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class SubscriptionPropertiesTest {

	@Autowired
	private SubscriptionProperties subscriptionProperties;

	@Test
	void defaultCurrencyIsJpy() {
		assertEquals("jpy", subscriptionProperties.getCurrency());
	}
}
