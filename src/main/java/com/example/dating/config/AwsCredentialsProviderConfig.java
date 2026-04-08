package com.example.dating.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;

@Configuration
public class AwsCredentialsProviderConfig {

	@Bean(destroyMethod = "")
	public AwsCredentialsProvider awsCredentialsProvider(
			@Value("${app.s3.access-key-id:}") String accessKeyId,
			@Value("${app.s3.secret-access-key:}") String secretAccessKey) {
		boolean hasStatic = !accessKeyId.isBlank() && !secretAccessKey.isBlank();
		if (hasStatic) {
			return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
		}
		// Env vars (include AWS_SESSION_TOKEN when using temporary creds), system props,
		// ~/.aws/credentials (profile from AWS_PROFILE or "default"), then container/EC2.
		return AwsCredentialsProviderChain.builder()
				.addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.addCredentialsProvider(SystemPropertyCredentialsProvider.create())
				.addCredentialsProvider(ProfileCredentialsProvider.create())
				.addCredentialsProvider(DefaultCredentialsProvider.create())
				.build();
	}
}
