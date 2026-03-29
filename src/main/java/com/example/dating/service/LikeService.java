package com.example.dating.service;


import com.example.dating.model.entity.LikeEntity;
import com.example.dating.model.entity.MatchEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.MatchRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {
	private final LikeRepo likeRepo;
	private final MatchRepo matchRepo;

	public LikeService(LikeRepo likeRepo, MatchRepo matchRepo) {
		this.likeRepo = likeRepo;
		this.matchRepo = matchRepo;
	}

	@Transactional
	public boolean likeAndMaybeMatch(Long from, Long to) {
		if (from.equals(to) || likeRepo.existsByFromUserAndToUser(from, to))
			return false;
		LikeEntity le = new LikeEntity();
		le.setFromUser(from);
		le.setToUser(to);
		likeRepo.save(le);
		if (likeRepo.existsByFromUserAndToUser(to, from)) {
			Long a = Math.min(from, to), b = Math.max(from, to);
			matchRepo.findByUserAAndUserB(a, b).orElseGet(() -> {
				MatchEntity m = new MatchEntity();
				m.setUserA(a);
				m.setUserB(b);
				return matchRepo.save(m);
			});
			return true;
		}
		return false;
	}
}
