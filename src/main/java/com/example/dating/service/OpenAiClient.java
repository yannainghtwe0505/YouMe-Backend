package com.example.dating.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.dating.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenAiClient {
	private final AiProperties props;
	private final ObjectMapper objectMapper;
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

	public OpenAiClient(AiProperties props, ObjectMapper objectMapper) {
		this.props = props;
		this.objectMapper = objectMapper;
	}

	public String chatCompletion(String systemPrompt, String userPrompt) throws Exception {
		if (!props.isEnabled() || !props.hasApiKey())
			return null;
		String base = props.getBaseUrl().replaceAll("/+$", "");
		URI uri = URI.create(base + "/chat/completions");
		Map<String, Object> body = Map.of(
				"model", props.getModel(),
				"temperature", 0.85,
				"max_tokens", 220,
				"messages", List.of(
						Map.of("role", "system", "content", systemPrompt),
						Map.of("role", "user", "content", userPrompt)));
		String json = objectMapper.writeValueAsString(body);
		HttpRequest req = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(45))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + props.getApiKey())
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();
		HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() < 200 || res.statusCode() >= 300)
			return null;
		JsonNode root = objectMapper.readTree(res.body());
		JsonNode choices = root.path("choices");
		if (!choices.isArray() || choices.isEmpty())
			return null;
		String text = choices.get(0).path("message").path("content").asText("");
		return text.isBlank() ? null : text.trim();
	}
}
