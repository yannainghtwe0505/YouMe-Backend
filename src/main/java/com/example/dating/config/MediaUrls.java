package com.example.dating.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaUrls {
	@Value("${app.media.public-base-url:https://example.com}")
	private String publicBaseUrl;

	public String urlForKey(String s3Key) {
		if (s3Key == null || s3Key.isBlank()) {
			return null;
		}
		String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
		return base + "/" + s3Key;
	}
}
