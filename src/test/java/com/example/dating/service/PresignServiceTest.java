package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

class PresignServiceTest {

	private PresignService presignService;

	@BeforeEach
	void setUp() {
		presignService = new PresignService(mock(AwsCredentialsProvider.class));
		ReflectionTestUtils.setField(presignService, "bucket", "test-bucket");
		ReflectionTestUtils.setField(presignService, "region", "us-east-1");
		ReflectionTestUtils.setField(presignService, "presignedPutExpirationMinutes", 15);
	}

	@Test
	void isReady_false_whenPresignerNotStarted() {
		ReflectionTestUtils.setField(presignService, "presignedPutEnabled", true);
		assertFalse(presignService.isReady());
	}

	@Test
	void presignUpload_throws_whenPresignerNull() {
		ReflectionTestUtils.setField(presignService, "presignedPutEnabled", true);
		ReflectionTestUtils.setField(presignService, "presigner", null);
		assertThrows(IllegalStateException.class, () -> presignService.presignUpload(1L, "photo.jpg", "image/jpeg"));
	}

	@Test
	void isReady_false_whenPresignedPutDisabled() {
		ReflectionTestUtils.setField(presignService, "presignedPutEnabled", false);
		ReflectionTestUtils.setField(presignService, "presigner", null);
		assertFalse(presignService.isReady());
	}
}
