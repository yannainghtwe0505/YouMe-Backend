package com.example.dating.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Public URLs for this deployment (API, SPA, mobile, WebSocket) and CORS. Override via
 * environment variables or profile-specific YAML — see docs/ENVIRONMENT_URLS.md.
 */
@ConfigurationProperties(prefix = "app.urls")
public class AppUrlsProperties {

	/**
	 * Origin clients use to reach this API (no trailing slash), e.g. https://api.example.com
	 */
	private String apiPublicBaseUrl = "";

	/**
	 * SPA origin for Stripe success/cancel redirects (no trailing slash).
	 */
	private String frontendBaseUrl = "";

	/**
	 * Optional mobile / deep-link base (e.g. universal link HTTPS or custom scheme docs).
	 */
	private String mobileBaseUrl = "";

	/**
	 * Optional full WebSocket URL (ws:// or wss://). Empty means clients derive from API URL + /ws/chat.
	 */
	private String websocketPublicBaseUrl = "";

	/**
	 * Comma-separated Spring CORS allowed origin patterns. Use {@code *} only for trusted local dev.
	 */
	private String corsAllowedOriginPatterns = "*";

	public String getApiPublicBaseUrl() {
		return apiPublicBaseUrl;
	}

	public void setApiPublicBaseUrl(String apiPublicBaseUrl) {
		this.apiPublicBaseUrl = apiPublicBaseUrl;
	}

	public String getFrontendBaseUrl() {
		return frontendBaseUrl;
	}

	public void setFrontendBaseUrl(String frontendBaseUrl) {
		this.frontendBaseUrl = frontendBaseUrl;
	}

	public String getMobileBaseUrl() {
		return mobileBaseUrl;
	}

	public void setMobileBaseUrl(String mobileBaseUrl) {
		this.mobileBaseUrl = mobileBaseUrl;
	}

	public String getWebsocketPublicBaseUrl() {
		return websocketPublicBaseUrl;
	}

	public void setWebsocketPublicBaseUrl(String websocketPublicBaseUrl) {
		this.websocketPublicBaseUrl = websocketPublicBaseUrl;
	}

	public String getCorsAllowedOriginPatterns() {
		return corsAllowedOriginPatterns;
	}

	public void setCorsAllowedOriginPatterns(String corsAllowedOriginPatterns) {
		this.corsAllowedOriginPatterns = corsAllowedOriginPatterns;
	}

	public List<String> corsAllowedOriginPatternsList() {
		return Arrays.stream(corsAllowedOriginPatterns.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}
}
