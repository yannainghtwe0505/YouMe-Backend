package com.example.dating.service;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class PresignService {
	@Value("${app.s3.bucket}")
	private String bucket;

	@Value("${app.s3.region}")
	private String region;

	@Value("${app.media.presigned-put.expiration-minutes:15}")
	private int presignedPutExpirationMinutes;

	@Value("${app.media.presigned-put.enabled:true}")
	private boolean presignedPutEnabled;

	private final AwsCredentialsProvider credentialsProvider;
	private S3Presigner presigner;

	public PresignService(AwsCredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	@PostConstruct
	void start() {
		if (!presignedPutEnabled) {
			return;
		}
		try {
			credentialsProvider.resolveCredentials();
		} catch (RuntimeException e) {
			throw new IllegalStateException(
					"S3 uploads require AWS credentials. Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY, "
							+ "use aws configure, or add application-local.yml (see application-local.example.yml). "
							+ "IAM needs s3:PutObject on uploads/* in the bucket.",
					e);
		}
		presigner = S3Presigner.builder()
				.region(Region.of(region))
				.credentialsProvider(credentialsProvider)
				.build();
	}

	@PreDestroy
	void stop() {
		if (presigner != null) {
			presigner.close();
			presigner = null;
		}
	}

	/** False when presigned PUT is disabled or failed to initialize (local dev without AWS). */
	public boolean isReady() {
		return presignedPutEnabled && presigner != null;
	}

	public record PresignResult(URI uploadUrl, String s3Key) {
	}

	public PresignResult presignUpload(Long userId, String filename, String contentType) {
		if (presigner == null) {
			throw new IllegalStateException(
					"Presigned upload is disabled. Set app.media.presigned-put.enabled=true and configure AWS credentials.");
		}
		String safe = filename == null || filename.isBlank() ? "photo.jpg" : filename;
		safe = safe.replaceAll("[^a-zA-Z0-9._-]", "_");
		String key = "uploads/" + userId + "/" + UUID.randomUUID() + "_" + safe;
		String ct = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;

		PutObjectRequest put = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(ct)
				.build();
		PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(Math.max(1, presignedPutExpirationMinutes)))
				.putObjectRequest(put)
				.build();
		PresignedPutObjectRequest signed = presigner.presignPutObject(presign);
		return new PresignResult(URI.create(signed.url().toString()), key);
	}
}
