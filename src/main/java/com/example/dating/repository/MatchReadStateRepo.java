package com.example.dating.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.MatchReadState;
import com.example.dating.model.entity.MatchReadStateId;

public interface MatchReadStateRepo extends JpaRepository<MatchReadState, MatchReadStateId> {
	Optional<MatchReadState> findByUserIdAndMatchId(Long userId, Long matchId);
}
