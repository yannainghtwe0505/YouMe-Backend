package com.example.dating.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.UserSubscriptionEntity;

public interface UserSubscriptionRepo extends JpaRepository<UserSubscriptionEntity, Long> {

	Optional<UserSubscriptionEntity> findByExternalSubscriptionId(String externalSubscriptionId);
}
