package com.example.dating.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.dating.model.entity.MatchReadState;
import com.example.dating.repository.MatchReadStateRepo;

@Service
public class MatchReadStateService {
	private final MatchReadStateRepo repo;

	public MatchReadStateService(MatchReadStateRepo repo) {
		this.repo = repo;
	}

	public void seedNewMatch(Long matchId, Long userA, Long userB) {
		Instant now = Instant.now();
		MatchReadState sA = new MatchReadState();
		sA.setUserId(userA);
		sA.setMatchId(matchId);
		sA.setLastReadAt(now);
		MatchReadState sB = new MatchReadState();
		sB.setUserId(userB);
		sB.setMatchId(matchId);
		sB.setLastReadAt(now);
		repo.save(sA);
		repo.save(sB);
	}

	public void markRead(Long userId, Long matchId) {
		Instant now = Instant.now();
		MatchReadState s = repo.findByUserIdAndMatchId(userId, matchId).orElseGet(() -> {
			MatchReadState n = new MatchReadState();
			n.setUserId(userId);
			n.setMatchId(matchId);
			return n;
		});
		s.setLastReadAt(now);
		repo.save(s);
	}
}
