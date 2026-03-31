package com.example.dating.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.PassEntity;

public interface PassRepo extends JpaRepository<PassEntity, Long> {
	boolean existsByFromUserAndToUser(Long fromUser, Long toUser);

	List<PassEntity> findByFromUser(Long fromUser);
}
