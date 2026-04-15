package com.example.dating.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Single CORS source for Spring MVC + Spring Security. JWT is sent in Authorization header only
 * (no cookies), so credentials can stay false - avoids browser/CORS edge cases with preflight PUTs.
 */
@Configuration
public class WebConfig {

	private final AppUrlsProperties appUrlsProperties;

	public WebConfig(AppUrlsProperties appUrlsProperties) {
		this.appUrlsProperties = appUrlsProperties;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		List<String> patterns = appUrlsProperties.corsAllowedOriginPatternsList();
		if (patterns.isEmpty()) {
			patterns = List.of("*");
		}
		config.setAllowedOriginPatterns(patterns);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Authorization"));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
