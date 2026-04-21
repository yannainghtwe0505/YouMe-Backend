package com.example.dating.service;

import java.util.ArrayList;
import java.util.List;

import com.example.dating.config.AiProperties;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

/**
 * Amazon Bedrock <a href="https://docs.aws.amazon.com/bedrock/latest/userguide/converse-api.html">Converse API</a>
 * — same message shape across foundation models (see AWS workshop notebook).
 */
public class BedrockChatClient implements ChatLlmClient {

	private final AiProperties props;
	private final BedrockRuntimeClient client;

	public BedrockChatClient(AiProperties props, AwsCredentialsProvider credentialsProvider) {
		this.props = props;
		String region = props.resolveBedrockRegion();
		this.client = BedrockRuntimeClient.builder()
				.region(Region.of(region))
				.credentialsProvider(credentialsProvider)
				.build();
	}

	@Override
	public String chatCompletion(String systemPrompt, String userPrompt, int maxTokens, double temperature)
			throws Exception {
		if (!props.isEnabled() || !props.isBedrockConfigured()) {
			return null;
		}
		int max = Math.max(32, Math.min(8192, maxTokens));
		float temp = (float) Math.max(0.0, Math.min(1.0, temperature));

		ConverseRequest.Builder req = ConverseRequest.builder()
				.modelId(props.getBedrockModelId().trim())
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(ContentBlock.fromText(userPrompt == null ? "" : userPrompt))
						.build())
				.inferenceConfig(InferenceConfiguration.builder()
						.maxTokens(max)
						.temperature(temp)
						.build());

		if (systemPrompt != null && !systemPrompt.isBlank()) {
			req.system(SystemContentBlock.builder().text(systemPrompt).build());
		}

		try {
			ConverseResponse response = client.converse(req.build());
			return extractText(response);
		} catch (SdkClientException e) {
			throw e;
		}
	}

	private static String extractText(ConverseResponse response) {
		if (response == null || response.output() == null || response.output().message() == null) {
			return null;
		}
		List<ContentBlock> blocks = response.output().message().content();
		if (blocks == null || blocks.isEmpty()) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		for (ContentBlock block : blocks) {
			if (block.text() != null && !block.text().isBlank()) {
				parts.add(block.text());
			}
		}
		if (parts.isEmpty()) {
			return null;
		}
		String joined = String.join("", parts).trim();
		return joined.isEmpty() ? null : joined;
	}

	@Override
	public void close() {
		if (client != null) {
			client.close();
		}
	}
}
