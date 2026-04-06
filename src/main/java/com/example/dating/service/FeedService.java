package com.example.dating.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.PassRepo;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.util.GeoUtils;

@Service
public class FeedService {
	private final ProfileRepo profiles;
	private final LikeRepo likeRepo;
	private final PassRepo passRepo;
	private final PhotoRepo photoRepo;
	private final MediaUrls mediaUrls;
	private final BlockService blockService;

	public FeedService(ProfileRepo profiles, LikeRepo likeRepo, PassRepo passRepo, PhotoRepo photoRepo,
			MediaUrls mediaUrls, BlockService blockService) {
		this.profiles = profiles;
		this.likeRepo = likeRepo;
		this.passRepo = passRepo;
		this.photoRepo = photoRepo;
		this.mediaUrls = mediaUrls;
		this.blockService = blockService;
	}

	public List<Map<String, Object>> feedForUser(Long myId) {
		Set<Long> likedTargets = likeRepo.findByFromUser(myId).stream()
				.map(l -> l.getToUser())
				.collect(Collectors.toCollection(HashSet::new));
		Set<Long> passedTargets = passRepo.findByFromUser(myId).stream()
				.map(p -> p.getToUser())
				.collect(Collectors.toCollection(HashSet::new));
		Set<Long> blockedPeers = blockService.hiddenPeerIdsFor(myId);

		List<ProfileEntity> candidates = profiles.findAll().stream()
				.filter(p -> !p.getUserId().equals(myId))
				.filter(p -> !likedTargets.contains(p.getUserId()))
				.filter(p -> !passedTargets.contains(p.getUserId()))
				.filter(p -> !blockedPeers.contains(p.getUserId()))
				.limit(50)
				.toList();

		final Double myLat;
		final Double myLon;
		var me = profiles.findById(myId);
		if (me.isPresent()) {
			myLat = me.get().getLatitude();
			myLon = me.get().getLongitude();
		} else {
			myLat = null;
			myLon = null;
		}

		Set<Long> ids = candidates.stream().map(ProfileEntity::getUserId).collect(Collectors.toSet());
		Map<Long, List<String>> photosByUser = new HashMap<>();
		if (!ids.isEmpty()) {
			Map<Long, List<PhotoEntity>> grouped = photoRepo.findByUserIdIn(ids).stream()
					.collect(Collectors.groupingBy(PhotoEntity::getUserId));
			for (Map.Entry<Long, List<PhotoEntity>> e : grouped.entrySet()) {
				List<String> urls = e.getValue().stream()
						.sorted(Comparator.comparing(PhotoEntity::getCreatedAt,
								Comparator.nullsLast(Comparator.naturalOrder())))
						.limit(6)
						.map(ph -> mediaUrls.urlForKey(ph.getS3Key()))
						.filter(Objects::nonNull)
						.toList();
				photosByUser.put(e.getKey(), urls);
			}
		}

		return candidates.stream()
				.map(p -> toCard(p, myLat, myLon, photosByUser.getOrDefault(p.getUserId(), List.of())))
				.toList();
	}

	private Map<String, Object> toCard(ProfileEntity p, Double myLat, Double myLon, List<String> photoUrls) {
		Map<String, Object> m = new HashMap<>();
		m.put("id", p.getUserId());
		m.put("userId", p.getUserId());
		m.put("name", p.getDisplayName());
		m.put("bio", p.getBio());
		m.put("location", p.getCity());
		m.put("city", p.getCity());
		m.put("gender", p.getGender());
		m.put("education", p.getEducation());
		m.put("occupation", p.getOccupation());
		m.put("hobbies", p.getHobbies());
		m.put("interests", p.getInterests());
		m.put("distanceKm", p.getDistanceKm());
		m.put("minAge", p.getMinAge());
		m.put("maxAge", p.getMaxAge());
		m.put("isPremium", p.isPremium());
		if (p.getBirthday() != null)
			m.put("age", Period.between(p.getBirthday(), LocalDate.now()).getYears());

		if (!photoUrls.isEmpty()) {
			m.put("photos", photoUrls);
			m.put("avatar", photoUrls.get(0));
		} else {
			m.put("photos", null);
			m.put("avatar", p.getPhotoUrl());
		}

		if (myLat != null && myLon != null && p.getLatitude() != null && p.getLongitude() != null) {
			int km = (int) Math.round(GeoUtils.haversineKm(myLat, myLon, p.getLatitude(), p.getLongitude()));
			m.put("distanceFromYouKm", km);
		}

		return m;
	}
}
