package com.example.dating.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import com.example.dating.util.DiscoveryJson;
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

		ProfileEntity me = profiles.findById(myId).orElse(null);
		if (me == null)
			return List.of();

		Map<Long, Long> photoCounts = loadPhotoCounts();

		List<ProfileEntity> candidates = collectCandidates(me, myId, likedTargets, passedTargets, blockedPeers,
				photoCounts, false, false);
		if (candidates.isEmpty() && expandDistanceWhenEmpty(me)) {
			candidates = collectCandidates(me, myId, likedTargets, passedTargets, blockedPeers, photoCounts, true,
					false);
		}
		if (candidates.isEmpty() && expandAgeWhenEmpty(me)) {
			boolean distRelaxed = expandDistanceWhenEmpty(me);
			candidates = collectCandidates(me, myId, likedTargets, passedTargets, blockedPeers, photoCounts,
					distRelaxed, true);
		}

		final Double myLat = me.getLatitude();
		final Double myLon = me.getLongitude();

		Set<Long> ids = candidates.stream().map(ProfileEntity::getUserId).collect(Collectors.toSet());
		Map<Long, List<String>> photosByUser = loadPhotosForUsers(ids);

		return candidates.stream()
				.map(p -> toCard(p, myLat, myLon, photosByUser.getOrDefault(p.getUserId(), List.of())))
				.toList();
	}

	private Map<Long, Long> loadPhotoCounts() {
		Map<Long, Long> m = new HashMap<>();
		for (Object[] row : photoRepo.countPhotosGroupByUserId()) {
			if (row[0] instanceof Number uid && row[1] instanceof Number cnt)
				m.put(uid.longValue(), cnt.longValue());
		}
		return m;
	}

	private Map<Long, List<String>> loadPhotosForUsers(Set<Long> ids) {
		Map<Long, List<String>> photosByUser = new HashMap<>();
		if (ids.isEmpty())
			return photosByUser;
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
		return photosByUser;
	}

	private List<ProfileEntity> collectCandidates(ProfileEntity me, Long myId, Set<Long> likedTargets,
			Set<Long> passedTargets, Set<Long> blockedPeers, Map<Long, Long> photoCounts, boolean relaxDistance,
			boolean relaxAge) {
		Double myLat = me.getLatitude();
		Double myLon = me.getLongitude();
		Integer myMaxKm = me.getDistanceKm();
		boolean applyKm = !relaxDistance && myMaxKm != null && myMaxKm > 0 && myLat != null && myLon != null;

		Map<String, Object> vd = DiscoveryJson.mapOrEmpty(me.getDiscoverySettings());
		Map<String, Object> filters = DiscoveryJson.nestedMap(vd, "filters");

		String rawMode = DiscoveryJson.str(vd.get("mode"));
		final String viewerMode = rawMode.isEmpty() ? "for_you" : rawMode;

		List<String> interestedRaw = DiscoveryJson.stringList(vd.get("interestedIn"));
		List<String> interestedNorm = interestedRaw.stream()
				.map(s -> DiscoveryJson.normGender(s))
				.filter(s -> !s.isEmpty())
				.toList();

		boolean requireBio = DiscoveryJson.truthy(vd.get("requireBio"));
		int minPhotos = Math.max(1, Math.min(6, DiscoveryJson.intValue(vd.get("minPhotos"), 1)));

		final Integer ageMinF = relaxAge ? null : me.getMinAge();
		final Integer ageMaxF = relaxAge ? null : me.getMaxAge();

		return profiles.findAll().stream()
				.filter(p -> !p.getUserId().equals(myId))
				.filter(p -> !likedTargets.contains(p.getUserId()))
				.filter(p -> !passedTargets.contains(p.getUserId()))
				.filter(p -> !blockedPeers.contains(p.getUserId()))
				.filter(p -> modeMatches(viewerMode, p))
				.filter(p -> genderMatches(interestedNorm, p))
				.filter(p -> bioMatches(requireBio, p))
				.filter(p -> photoCountMatches(p, minPhotos, photoCounts))
				.filter(p -> ageMatches(ageMinF, ageMaxF, p))
				.filter(p -> withinDiscoveryRadius(myLat, myLon, applyKm, myMaxKm, p))
				.filter(p -> dealbreakerFiltersMatch(filters, p))
				.sorted(Comparator.comparingDouble((ProfileEntity p) -> distanceSortKey(myLat, myLon, p)))
				.limit(50)
				.toList();
	}

	private static boolean expandDistanceWhenEmpty(ProfileEntity me) {
		return DiscoveryJson.truthy(DiscoveryJson.mapOrEmpty(me.getDiscoverySettings()).get("expandDistanceWhenEmpty"));
	}

	private static boolean expandAgeWhenEmpty(ProfileEntity me) {
		return DiscoveryJson.truthy(DiscoveryJson.mapOrEmpty(me.getDiscoverySettings()).get("expandAgeWhenEmpty"));
	}

	private static boolean modeMatches(String viewerMode, ProfileEntity candidate) {
		Map<String, Object> life = DiscoveryJson.mapOrEmpty(candidate.getLifestyle());
		List<String> appears = DiscoveryJson.stringList(life.get("appearsInModes"));
		if (appears.isEmpty())
			return true;
		String vm = viewerMode.toLowerCase(Locale.ROOT);
		return appears.stream().anyMatch(a -> vm.equalsIgnoreCase(String.valueOf(a).trim()));
	}

	private static boolean genderMatches(List<String> interestedNorm, ProfileEntity candidate) {
		if (interestedNorm.isEmpty())
			return true;
		String cg = DiscoveryJson.normGender(candidate.getGender());
		return DiscoveryJson.gendersOverlap(interestedNorm, cg);
	}

	private static boolean bioMatches(boolean requireBio, ProfileEntity candidate) {
		if (!requireBio)
			return true;
		String b = candidate.getBio();
		return b != null && !b.trim().isEmpty();
	}

	private static int effectivePhotoCount(ProfileEntity p, Map<Long, Long> photoCounts) {
		long table = photoCounts.getOrDefault(p.getUserId(), 0L);
		int c = (int) Math.min(Integer.MAX_VALUE, table);
		if (c == 0 && p.getPhotoUrl() != null && !p.getPhotoUrl().isBlank())
			c = 1;
		return c;
	}

	private static boolean photoCountMatches(ProfileEntity p, int minPhotos, Map<Long, Long> photoCounts) {
		return effectivePhotoCount(p, photoCounts) >= minPhotos;
	}

	private static boolean ageMatches(Integer min, Integer max, ProfileEntity candidate) {
		if (min == null && max == null)
			return true;
		if (candidate.getBirthday() == null)
			return false;
		int age = Period.between(candidate.getBirthday(), LocalDate.now()).getYears();
		if (min != null && age < min)
			return false;
		if (max != null && age > max)
			return false;
		return true;
	}

	private static String candTrait(ProfileEntity p, Map<String, Object> life, String key) {
		Object o = life.get(key);
		if (o != null && !DiscoveryJson.str(o).isEmpty())
			return DiscoveryJson.str(o);
		return switch (key) {
			case "education" -> DiscoveryJson.str(p.getEducation());
			case "occupation" -> DiscoveryJson.str(p.getOccupation());
			default -> "";
		};
	}

	private static boolean dealbreakerFiltersMatch(Map<String, Object> filters, ProfileEntity candidate) {
		if (filters.isEmpty())
			return true;
		Map<String, Object> life = DiscoveryJson.mapOrEmpty(candidate.getLifestyle());

		String[] stringKeys = { "lookingFor", "zodiac", "education", "familyPlans", "communicationStyle", "loveStyle",
				"pets", "drinking", "smoking", "workout", "socialMedia" };
		for (String k : stringKeys) {
			String want = DiscoveryJson.str(filters.get(k));
			if (want.isEmpty())
				continue;
			String has = candTrait(candidate, life, k);
			if (has.isEmpty() && "education".equals(k))
				has = DiscoveryJson.str(candidate.getEducation());
			if (!DiscoveryJson.stringPrefMatch(want, has))
				return false;
		}

		List<String> wantLang = DiscoveryJson.stringList(filters.get("languages"));
		if (!wantLang.isEmpty()) {
			List<String> haveLang = DiscoveryJson.stringList(life.get("languages"));
			if (!DiscoveryJson.langsOverlap(wantLang, haveLang))
				return false;
		}

		String mustInterest = DiscoveryJson.str(filters.get("mustShareInterest"));
		if (!mustInterest.isEmpty()) {
			List<String> ci = candidate.getInterests();
			if (!DiscoveryJson.interestContains(ci, mustInterest))
				return false;
		}

		return true;
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
		if (p.getLifestyle() != null && !p.getLifestyle().isEmpty())
			m.put("lifestyle", p.getLifestyle());

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

	private static boolean withinDiscoveryRadius(Double myLat, Double myLon, boolean applyFilter, Integer maxKm,
			ProfileEntity p) {
		if (!applyFilter || maxKm == null)
			return true;
		if (p.getLatitude() == null || p.getLongitude() == null)
			return false;
		double d = GeoUtils.haversineKm(myLat, myLon, p.getLatitude(), p.getLongitude());
		return d <= maxKm + 1e-6;
	}

	private static double distanceSortKey(Double myLat, Double myLon, ProfileEntity p) {
		if (myLat == null || myLon == null)
			return 0;
		if (p.getLatitude() == null || p.getLongitude() == null)
			return 9e9;
		return GeoUtils.haversineKm(myLat, myLon, p.getLatitude(), p.getLongitude());
	}
}
