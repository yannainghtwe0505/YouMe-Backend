package com.example.dating.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.PhotoEntity;

public interface PhotoRepo extends JpaRepository<PhotoEntity, Long> {
	List<PhotoEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

	List<PhotoEntity> findByUserIdIn(Collection<Long> userIds);

	long countByUserId(Long userId);
}
