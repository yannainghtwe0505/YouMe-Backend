package com.example.dating.service;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

class S3ObjectDeleteServiceTest {

	@Test
	void deleteKeyBestEffort_noOp_whenClientNotInitialized() {
		AwsCredentialsProvider creds = mock(AwsCredentialsProvider.class);
		S3ObjectDeleteService svc = new S3ObjectDeleteService(creds);
		Assertions.assertDoesNotThrow(() -> svc.deleteKeyBestEffort("some/key"));
	}

	@Test
	void deleteKeyBestEffort_noOp_whenKeyBlank() {
		AwsCredentialsProvider creds = mock(AwsCredentialsProvider.class);
		S3ObjectDeleteService svc = new S3ObjectDeleteService(creds);
		Assertions.assertDoesNotThrow(() -> svc.deleteKeyBestEffort("  "));
		Assertions.assertDoesNotThrow(() -> svc.deleteKeyBestEffort(null));
	}
}
