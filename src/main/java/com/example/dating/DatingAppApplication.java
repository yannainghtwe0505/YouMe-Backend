package com.example.dating;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.example.dating.repository")
public class DatingAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(DatingAppApplication.class, args);
	}

	@Bean
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
