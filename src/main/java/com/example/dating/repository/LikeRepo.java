package com.example.dating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.example.dating.model.entity.LikeEntity;

public interface LikeRepo extends JpaRepository<LikeEntity, Long> {
	boolean existsByFromUserAndToUser(Long from, Long to);

	List<LikeEntity> findByFromUser(Long fromUser);

	List<LikeEntity> findByToUser(Long toUser);

	/** Idempotent insert: no error if this (from,to) like already exists. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "INSERT INTO likes (from_user, to_user, super_like, created_at) "
			+ "VALUES (:fromUser, :toUser, :superLike, NOW()) "
			+ "ON CONFLICT (from_user, to_user) DO NOTHING", nativeQuery = true)
	int insertLikeIdempotent(@Param("fromUser") Long fromUser, @Param("toUser") Long toUser,
			@Param("superLike") boolean superLike);
}
