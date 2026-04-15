package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

class S3ObjectPutServiceTest {

	@Test
	void isAvailable_false_beforePostConstruct() {
		AwsCredentialsProvider creds = StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "y"));
		S3ObjectPutService svc = new S3ObjectPutService(creds);
		assertFalse(svc.isAvailable());
	}

	@Test
	void putObject_throws_whenClientNotInitialized() {
		AwsCredentialsProvider creds = mock(AwsCredentialsProvider.class);
		S3ObjectPutService svc = new S3ObjectPutService(creds);
		assertThrows(IllegalStateException.class, () -> svc.putObject("k", new byte[] { 1 }, "image/jpeg"));
	}
}
