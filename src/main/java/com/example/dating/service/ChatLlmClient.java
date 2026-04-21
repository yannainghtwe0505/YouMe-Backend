package com.example.dating.service;

/**
 * Text generation for AI coach features (reply ideas, profile tips, match greeting, etc.).
 * Backed by Amazon Bedrock Converse API or an OpenAI-compatible HTTP API.
 */
public interface ChatLlmClient extends AutoCloseable {

	String chatCompletion(String systemPrompt, String userPrompt, int maxTokens, double temperature) throws Exception;

	@Override
	default void close() {
		// Bedrock implementation closes the runtime client; OpenAI has no resources.
	}
}
