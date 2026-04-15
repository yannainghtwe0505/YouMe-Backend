package com.example.dating.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Optional Redis for {@code youme:ai:{userId}:{feature}:{date}} counters. When disabled, usage is JDBC-only.
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.redis-usage-enabled", havingValue = "true")
public class RedisAiUsageConfiguration {

	@Bean
	LettuceConnectionFactory redisConnectionFactory(AiProperties props) {
		var redis = props.getRedis();
		var conf = new RedisStandaloneConfiguration(redis.getHost(), redis.getPort());
		return new LettuceConnectionFactory(conf);
	}

	@Bean
	StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(redisConnectionFactory);
		return t;
	}
}
