package com.example.dating.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.PassRepo;
import com.example.dating.repository.ProfileRepo;

@Service
public class FeedService {
	private final ProfileRepo profiles;
	private final LikeRepo likeRepo;
	private final PassRepo passRepo;

	public FeedService(ProfileRepo profiles, LikeRepo likeRepo, PassRepo passRepo) {
		this.profiles = profiles;
		this.likeRepo = likeRepo;
		this.passRepo = passRepo;
	}

	public List<Map<String, Object>> feedForUser(Long myId) {
		Set<Long> likedTargets = likeRepo.findByFromUser(myId).stream()
				.map(l -> l.getToUser())
				.collect(Collectors.toCollection(HashSet::new));
		Set<Long> passedTargets = passRepo.findByFromUser(myId).stream()
				.map(p -> p.getToUser())
				.collect(Collectors.toCollection(HashSet::new));

		return profiles.findAll().stream()
				.filter(p -> !p.getUserId().equals(myId))
				.filter(p -> !likedTargets.contains(p.getUserId()))
				.filter(p -> !passedTargets.contains(p.getUserId()))
				.limit(50)
				.map(p -> toCard(p))
				.toList();
	}

	private static Map<String, Object> toCard(ProfileEntity p) {
		Map<String, Object> m = new HashMap<>();
		m.put("id", p.getUserId());
		m.put("userId", p.getUserId());
		m.put("name", p.getDisplayName());
		m.put("bio", p.getBio());
		m.put("location", p.getCity());
		m.put("avatar", p.getPhotoUrl());
		if (p.getBirthday() != null)
			m.put("age", Period.between(p.getBirthday(), LocalDate.now()).getYears());
		return m;
	}
}
