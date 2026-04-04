package com.example.dating.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Component
public class MediaUrls {
	@Value("${app.media.public-base-url:https://example.com}")
	private String publicBaseUrl;

	@Value("${app.media.presigned-get.enabled:false}")
	private boolean presignedGetEnabled;

	@Value("${app.media.presigned-get.expiration-minutes:60}")
	private int presignedExpirationMinutes;

	@Value("${app.s3.bucket}")
	private String bucket;

	@Value("${app.s3.region}")
	private String region;

	private final AwsCredentialsProvider credentialsProvider;

	private S3Presigner presigner;

	public MediaUrls(AwsCredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	@PostConstruct
	void startPresigner() {
		if (!presignedGetEnabled) {
			return;
		}
		try {
			credentialsProvider.resolveCredentials();
		} catch (RuntimeException e) {
			throw new IllegalStateException(
					"app.media.presigned-get.enabled is true but no AWS credentials were found. "
							+ "Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY (and AWS_SESSION_TOKEN for temporary creds), "
							+ "run aws configure, set AWS_PROFILE if not using [default], "
							+ "or add application-local.yml next to pom.xml (see application-local.example.yml).",
					e);
		}
		presigner = S3Presigner.builder()
				.region(Region.of(region))
				.credentialsProvider(credentialsProvider)
				.build();
	}

	@PreDestroy
	void stopPresigner() {
		if (presigner != null) {
			presigner.close();
			presigner = null;
		}
	}

	public String urlForKey(String s3Key) {
		if (s3Key == null || s3Key.isBlank()) {
			return null;
		}
		String key = normalizeToObjectKey(s3Key.strip());
		if (key.isBlank()) {
			return null;
		}
		if (presignedGetEnabled && presigner != null) {
			return presignGet(key);
		}
		String encodedPath = Arrays.stream(key.split("/"))
				.map(seg -> UriUtils.encodePathSegment(seg, StandardCharsets.UTF_8))
				.collect(Collectors.joining("/"));
		String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
				: publicBaseUrl;
		return base + "/" + encodedPath;
	}

	private String presignGet(String objectKey) {
		GetObjectRequest get = GetObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.build();
		GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(Math.max(1, presignedExpirationMinutes)))
				.getObjectRequest(get)
				.build();
		PresignedGetObjectRequest signed = presigner.presignGetObject(presign);
		return signed.url().toExternalForm();
	}

	private static String normalizeToObjectKey(String raw) {
		if (!raw.regionMatches(true, 0, "s3://", 0, 5)) {
			return raw;
		}
		String rest = raw.substring(5);
		int slash = rest.indexOf('/');
		if (slash < 0 || slash >= rest.length() - 1) {
			return raw;
		}
		return rest.substring(slash + 1);
	}
}
