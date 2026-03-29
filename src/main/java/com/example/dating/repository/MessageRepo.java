package com.example.dating.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.MessageEntity;

import java.util.List;

public interface MessageRepo extends JpaRepository<MessageEntity, Long> {
	List<MessageEntity> findByMatchIdOrderByCreatedAtAsc(Long matchId, Pageable pageable);
}
