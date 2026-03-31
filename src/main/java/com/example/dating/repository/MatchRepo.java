package com.example.dating.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.dating.model.entity.MatchEntity;

public interface MatchRepo extends JpaRepository<MatchEntity, Long> {
	Optional<MatchEntity> findByUserAAndUserB(Long a, Long b);

	boolean existsByUserAAndUserB(Long a, Long b);

	@Query("SELECT m FROM MatchEntity m WHERE m.userA = :userId OR m.userB = :userId ORDER BY m.createdAt DESC")
	List<MatchEntity> findAllByUserInvolved(@Param("userId") Long userId);
}
