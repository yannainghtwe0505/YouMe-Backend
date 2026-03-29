package com.example.dating.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.MatchEntity;

public interface MatchRepo extends JpaRepository<MatchEntity, Long> {
	Optional<MatchEntity> findByUserAAndUserB(Long a, Long b);

	boolean existsByUserAAndUserB(Long a, Long b);
}
