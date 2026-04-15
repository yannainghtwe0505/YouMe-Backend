package com.example.dating.config;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(EntityManagerFactory.class)
@EnableJpaRepositories("com.example.dating.repository")
public class JpaRepositoriesConfig {
}
