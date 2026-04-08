package com.example.dating.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.MessageEntity;

public interface MessageRepo extends JpaRepository<MessageEntity, Long> {
	long countByMatchId(Long matchId);

	List<MessageEntity> findByMatchIdOrderByCreatedAtAsc(Long matchId, Pageable pageable);

	Optional<MessageEntity> findFirstByMatchIdOrderByCreatedAtDesc(Long matchId);

	long countByMatchIdAndSenderIdNotAndCreatedAtAfter(Long matchId, Long senderId, Instant createdAt);
}
