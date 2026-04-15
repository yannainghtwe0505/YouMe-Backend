package com.example.dating;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = { RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class })
public class DatingAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(DatingAppApplication.class, args);
	}

	@Bean
	@ConditionalOnBean(DataSource.class)
	CommandLineRunner testDb(DataSource dataSource) {
		return args -> {
			try (Connection conn = dataSource.getConnection()) {
				System.out.println("Connected to DB: " + conn.getMetaData().getDatabaseProductName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}
}
