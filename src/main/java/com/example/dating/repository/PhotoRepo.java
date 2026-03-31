package com.example.dating.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.PhotoEntity;

public interface PhotoRepo extends JpaRepository<PhotoEntity, Long> {
	List<PhotoEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

	long countByUserId(Long userId);
}
