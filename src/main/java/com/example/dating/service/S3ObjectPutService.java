package com.example.dating.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3ObjectPutService {

	@Value("${app.s3.bucket}")
	private String bucket;

	@Value("${app.s3.region}")
	private String region;

	private final AwsCredentialsProvider credentialsProvider;
	private S3Client s3;

	public S3ObjectPutService(AwsCredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	@PostConstruct
	void start() {
		s3 = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(credentialsProvider)
				.build();
	}

	@PreDestroy
	void stop() {
		if (s3 != null) {
			s3.close();
			s3 = null;
		}
	}

	public void putObject(String key, byte[] body, String contentType) {
		if (s3 == null) {
			throw new IllegalStateException("S3 client not initialized");
		}
		String ct = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
		s3.putObject(
				PutObjectRequest.builder().bucket(bucket).key(key).contentType(ct).build(),
				RequestBody.fromBytes(body));
	}

	public boolean isAvailable() {
		return s3 != null;
	}
}
