package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.config.SubscriptionProperties;
import com.example.dating.repository.UserSubscriptionRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileIapVerificationServiceTest {

	@Mock
	private SubscriptionProperties subscriptionProperties;

	@Mock
	private UserSubscriptionRepo userSubscriptionRepo;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void verifyIos_throwsServiceUnavailable_whenAppleNotConfigured() {
		when(subscriptionProperties.appleIapConfigured()).thenReturn(false);
		MobileIapVerificationService svc = new MobileIapVerificationService(subscriptionProperties, objectMapper,
				userSubscriptionRepo);
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> svc.verifyIos(1L, "dGVzdA=="));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
	}

	@Test
	void verifyIos_throwsBadRequest_whenReceiptBlank() {
		when(subscriptionProperties.appleIapConfigured()).thenReturn(true);
		MobileIapVerificationService svc = new MobileIapVerificationService(subscriptionProperties, objectMapper,
				userSubscriptionRepo);
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> svc.verifyIos(1L, "  "));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void verifyAndroid_throwsWhenGoogleNotConfigured() {
		when(subscriptionProperties.googlePlayConfigured()).thenReturn(false);
		MobileIapVerificationService svc = new MobileIapVerificationService(subscriptionProperties, objectMapper,
				userSubscriptionRepo);
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> svc.verifyAndroid(1L, "plus", "token"));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
	}
}