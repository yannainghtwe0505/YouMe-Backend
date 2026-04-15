package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.repository.PendingRegistrationRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.security.JwtService;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingRegistrationServiceTest {

	@Mock
	private PendingRegistrationRepo pendingRepo;

	@Mock
	private UserRepo users;

	@Mock
	private ProfileRepo profiles;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwt;

	@Mock
	private AccountRegistrationService accountRegistration;

	@Mock
	private RegistrationOtpDeliveryService otpDelivery;

	@Test
	void normalizeEmail_trimsAndLowercases() {
		assertEquals("pat@example.com", OnboardingRegistrationService.normalizeEmail("  Pat@Example.COM "));
	}

	@Test
	void normalizeEmail_blankBecomesNull() {
		assertNull(OnboardingRegistrationService.normalizeEmail("   "));
	}

	@Test
	void normalizeJpPhone_convertsDomesticMobileToE164() {
		assertEquals("+819012345678", OnboardingRegistrationService.normalizeJpPhone("090-1234-5678"));
	}

	@Test
	void sendEmailCode_throwsWhenInvalidEmail() {
		OnboardingRegistrationService svc = new OnboardingRegistrationService(pendingRepo, users, profiles,
				passwordEncoder, jwt, accountRegistration, otpDelivery, "");
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> svc.sendEmailCode("not-email"));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		verify(pendingRepo, never()).save(any());
	}

	@Test
	void sendEmailCode_throwsConflictWhenEmailRegistered() {
		OnboardingRegistrationService svc = new OnboardingRegistrationService(pendingRepo, users, profiles,
				passwordEncoder, jwt, accountRegistration, otpDelivery, "");
		when(users.findByEmail("taken@example.com")).thenReturn(Optional.of(new com.example.dating.model.entity.UserEntity()));
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> svc.sendEmailCode("taken@example.com"));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
		verify(otpDelivery, never()).sendEmailVerificationCode(anyString(), anyString());
	}
}