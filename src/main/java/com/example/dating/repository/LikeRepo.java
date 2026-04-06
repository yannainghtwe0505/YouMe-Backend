package com.example.dating.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.example.dating.model.entity.LikeEntity;

public interface LikeRepo extends JpaRepository<LikeEntity, Long> {
	boolean existsByFromUserAndToUser(Long from, Long to);

	List<LikeEntity> findByFromUser(Long fromUser);

	List<LikeEntity> findByToUser(Long toUser);
}
