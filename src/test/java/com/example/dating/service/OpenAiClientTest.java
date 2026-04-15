package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenAiClientTest {

	@Mock
	private AiProperties props;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void chatCompletion_returnsNull_whenAiDisabled() throws Exception {
		when(props.isEnabled()).thenReturn(false);
		OpenAiClient client = new OpenAiClient(props, objectMapper);
		assertNull(client.chatCompletion("sys", "user"));
	}

	@Test
	void chatCompletion_returnsNull_whenApiKeyMissing() throws Exception {
		when(props.isEnabled()).thenReturn(true);
		when(props.hasApiKey()).thenReturn(false);
		OpenAiClient client = new OpenAiClient(props, objectMapper);
		assertNull(client.chatCompletion("sys", "user"));
	}
}