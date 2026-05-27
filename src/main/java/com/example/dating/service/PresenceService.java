package com.example.dating.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.UserRepo;

@Service
public class PresenceService {
	private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(3);

	private final UserRepo userRepo;

	public PresenceService(UserRepo userRepo) {
		this.userRepo = userRepo;
	}

	@Transactional
	public void touch(Long userId) {
		UserEntity user = userRepo.findById(userId).orElse(null);
		if (user == null) {
			return;
		}
		user.setLastActiveAt(Instant.now());
		userRepo.save(user);
	}

	public Optional<Instant> lastActiveAt(Long userId) {
		return userRepo.findById(userId).map(UserEntity::getLastActiveAt);
	}

	public boolean isOnline(Instant lastActiveAt) {
		if (lastActiveAt == null) {
			return false;
		}
		return lastActiveAt.isAfter(Instant.now().minus(ONLINE_THRESHOLD));
	}
}
