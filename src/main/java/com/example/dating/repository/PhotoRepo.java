package com.example.dating.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.dating.model.entity.PhotoEntity;

public interface PhotoRepo extends JpaRepository<PhotoEntity, Long> {
	Optional<PhotoEntity> findByIdAndUserId(Long id, Long userId);

	List<PhotoEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

	List<PhotoEntity> findByUserIdIn(Collection<Long> userIds);

	long countByUserId(Long userId);

	@Query("select p.userId, count(p) from PhotoEntity p group by p.userId")
	List<Object[]> countPhotosGroupByUserId();
}
