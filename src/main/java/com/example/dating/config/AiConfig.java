package com.example.dating.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.dating.service.BedrockChatClient;
import com.example.dating.service.ChatLlmClient;
import com.example.dating.service.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

	@Bean(destroyMethod = "close")
	public ChatLlmClient chatLlmClient(AiProperties props, ObjectMapper objectMapper,
			AwsCredentialsProvider awsCredentialsProvider) {
		if (props.isOpenAiProvider()) {
			return new OpenAiClient(props, objectMapper);
		}
		return new BedrockChatClient(props, awsCredentialsProvider);
	}
}
