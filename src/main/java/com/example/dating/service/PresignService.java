package com.example.dating.service;

import java.net.URI;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PresignService {
	@Value("${app.s3.bucket}")
	private String bucket;

	public record PresignResult(URI uploadUrl, String s3Key) {
	}

	public PresignResult presignUpload(Long userId, String filename, String contentType) {
		String safe = filename == null ? "photo.jpg" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
		String key = "uploads/" + userId + "/" + UUID.randomUUID() + "_" + safe;
		URI url = URI.create("https://example-s3/" + bucket + "/" + key + "?presigned");
		return new PresignResult(url, key);
	}
}
