package com.example.dating.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.PendingRegistrationEntity;

public interface PendingRegistrationRepo extends JpaRepository<PendingRegistrationEntity, Long> {
	Optional<PendingRegistrationEntity> findByEmail(String email);

	Optional<PendingRegistrationEntity> findByPhoneE164(String phoneE164);

	Optional<PendingRegistrationEntity> findBySessionToken(String sessionToken);
}
