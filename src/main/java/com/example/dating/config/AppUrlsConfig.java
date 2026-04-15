package com.example.dating.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppUrlsProperties.class)
public class AppUrlsConfig {
}
