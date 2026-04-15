package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationOtpDeliveryServiceTest {

	@Mock
	private ObjectProvider<JavaMailSender> javaMailSender;

	@Mock
	private Environment environment;

	private RegistrationOtpDeliveryService service;

	@BeforeEach
	void setUp() {
		when(javaMailSender.getIfAvailable()).thenReturn(null);
		when(environment.getProperty("spring.mail.host")).thenReturn(null);
		when(environment.acceptsProfiles(Profiles.of("dev", "test"))).thenReturn(false);
		service = new RegistrationOtpDeliveryService(javaMailSender, environment);
		ReflectionTestUtils.setField(service, "emailFrom", "noreply@test.local");
		ReflectionTestUtils.setField(service, "logOtpProperty", true);
	}

	@Test
	void sendEmailVerificationCode_doesNotThrow_whenSmtpMissingButLogOtpEnabled() {
		assertDoesNotThrow(() -> service.sendEmailVerificationCode("user@example.com", "123456"));
	}

	@Test
	void sendEmailVerificationCode_throwsWhenNoSmtpAndLogOtpDisabled() {
		ReflectionTestUtils.setField(service, "logOtpProperty", false);
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> service.sendEmailVerificationCode("user@example.com", "123456"));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
	}
}