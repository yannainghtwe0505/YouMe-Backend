package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.config.MediaUrls;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.security.JwtService;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountRegistrationServiceTest {

	@Mock
	private UserRepo users;
	@Mock
	private ProfileRepo profiles;
	@Mock
	private PhotoRepo photos;
	@Mock
	private PasswordEncoder encoder;
	@Mock
	private JwtService jwt;
	@Mock
	private S3ObjectPutService s3Put;
	@Mock
	private MediaUrls mediaUrls;

	@InjectMocks
	private AccountRegistrationService accountRegistrationService;

	@Test
	void isS3Available_delegatesToPutService() {
		when(s3Put.isAvailable()).thenReturn(true);
		assertTrue(accountRegistrationService.isS3Available());
	}

	@Test
	void registerWithProfile_throwsServiceUnavailable_whenS3Down() {
		when(s3Put.isAvailable()).thenReturn(false);
		MultipartFile[] photosArr = { jpegFile() };
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> accountRegistrationService.registerWithProfile("pat@example.com", "secret12", "Pat Lee", photosArr));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
	}

	@Test
	void registerWithProfile_throwsBadRequest_onInvalidEmail() {
		when(s3Put.isAvailable()).thenReturn(true);
		MultipartFile[] photosArr = { jpegFile() };
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> accountRegistrationService.registerWithProfile("not-an-email", "secret12", "Pat Lee", photosArr));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void registerWithProfile_throwsBadRequest_onShortPassword() {
		when(s3Put.isAvailable()).thenReturn(true);
		MultipartFile[] photosArr = { jpegFile() };
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> accountRegistrationService.registerWithProfile("pat@example.com", "short", "Pat Lee", photosArr));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	private static MultipartFile jpegFile() {
		MultipartFile f = mock(MultipartFile.class);
		when(f.isEmpty()).thenReturn(false);
		when(f.getContentType()).thenReturn("image/jpeg");
		return f;
	}
}