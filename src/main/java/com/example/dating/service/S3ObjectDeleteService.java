package com.example.dating.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
public class S3ObjectDeleteService {
	private static final Logger log = LoggerFactory.getLogger(S3ObjectDeleteService.class);

	@Value("${app.s3.bucket}")
	private String bucket;

	@Value("${app.s3.region}")
	private String region;

	private final AwsCredentialsProvider credentialsProvider;
	private S3Client s3;

	public S3ObjectDeleteService(AwsCredentialsProvider credentialsProvider) {
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

	public void deleteKeyBestEffort(String s3Key) {
		if (s3Key == null || s3Key.isBlank() || s3 == null) {
			return;
		}
		try {
			s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build());
		} catch (Exception e) {
			log.warn("Could not delete S3 object {}: {}", s3Key, e.getMessage());
		}
	}
}
