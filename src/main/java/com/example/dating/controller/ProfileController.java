package com.example.dating.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import com.example.dating.config.MediaUrls;
import com.example.dating.dto.AiQuotaView;
import com.example.dating.dto.ChangePasswordRequest;
import com.example.dating.dto.MeResponse;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.service.AiCoachService;
import com.example.dating.service.OnboardingRegistrationService;
import com.example.dating.service.ProfileAvatarService;
import com.example.dating.service.SubscriptionPlanService;
import com.example.dating.service.TieredAiUsageService;
import com.example.dating.service.UserSubscriptionService;

import com.example.dating.model.entity.UserEntity;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/me")
@Validated
public class ProfileController {
	private final ProfileRepo repo;
	private final UserRepo users;
	private final PhotoRepo photoRepo;
	private final MediaUrls mediaUrls;
	private final PasswordEncoder passwordEncoder;
	private final OnboardingRegistrationService onboarding;
	private final AiCoachService aiCoachService;
	private final TieredAiUsageService tieredAiUsageService;
	private final SubscriptionPlanService subscriptionPlanService;
	private final UserSubscriptionService userSubscriptionService;
	private final ProfileAvatarService profileAvatarService;

	public ProfileController(ProfileRepo repo, UserRepo users, PhotoRepo photoRepo, MediaUrls mediaUrls,
			PasswordEncoder passwordEncoder, OnboardingRegistrationService onboarding, AiCoachService aiCoachService,
			TieredAiUsageService tieredAiUsageService, SubscriptionPlanService subscriptionPlanService,
			UserSubscriptionService userSubscriptionService, ProfileAvatarService profileAvatarService) {
		this.repo = repo;
		this.users = users;
		this.photoRepo = photoRepo;
		this.mediaUrls = mediaUrls;
		this.passwordEncoder = passwordEncoder;
		this.onboarding = onboarding;
		this.aiCoachService = aiCoachService;
		this.tieredAiUsageService = tieredAiUsageService;
		this.subscriptionPlanService = subscriptionPlanService;
		this.userSubscriptionService = userSubscriptionService;
		this.profileAvatarService = profileAvatarService;
	}

	@PutMapping("/password")
	public ResponseEntity<?> changePassword(@AuthenticationPrincipal User me,
			@Valid @RequestBody ChangePasswordRequest req) {
		Long id = Long.valueOf(me.getUsername());
		UserEntity u = users.findById(id).orElse(null);
		if (u == null) {
			return ResponseEntity.status(404).body(Map.of("error", "user not found"));
		}
		if (!passwordEncoder.matches(req.currentPassword, u.getPasswordHash())) {
			return ResponseEntity.badRequest().body(Map.of("error", "current password is incorrect"));
		}
		u.setPasswordHash(passwordEncoder.encode(req.newPassword));
		users.save(u);
		return ResponseEntity.ok(Map.of("ok", true));
	}

	@DeleteMapping
	public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal User me) {
		Long id = Long.valueOf(me.getUsername());
		if (!users.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		users.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<?> get(@AuthenticationPrincipal User me) {
		String sub = me.getUsername();
		if (sub.startsWith("pending:")) {
			return ResponseEntity.ok(onboarding.meForPendingPrincipal(Long.parseLong(sub.substring(8))));
		}
		Long id = Long.valueOf(sub);
		if (!repo.existsById(id))
			return ResponseEntity.notFound().build();
		var user = users.findById(id).orElseThrow();
		var profile = repo.findById(id).orElseThrow();
		List<String> photoUrls = photoRepo.findByUserIdOrderByCreatedAtAsc(id).stream()
				.map(ph -> mediaUrls.urlForKey(ph.getS3Key()))
				.filter(u -> u != null)
				.limit(6)
				.toList();
		var plan = subscriptionPlanService.resolve(profile);
		AiQuotaView chatQuota = tieredAiUsageService.status(id, plan, AiFeature.CHAT_REPLY);
		var entitlements = tieredAiUsageService.entitlementsMap(id, plan);
		String resolvedAvatar = profileAvatarService.resolveAvatarUrl(id, profile);
		return ResponseEntity.ok(
				MeResponse.from(user, profile, photoUrls, resolvedAvatar, chatQuota, plan.name(), entitlements));
	}

	public static class LocaleBody {
		public String locale;
	}

	@PutMapping("/locale")
	public ResponseEntity<?> setLocale(@AuthenticationPrincipal User me, @RequestBody LocaleBody body) {
		if (body == null || body.locale == null || body.locale.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "locale is required"));
		}
		String lc = body.locale.trim().toLowerCase();
		if (!"en".equals(lc) && !"ja".equals(lc) && !"my".equals(lc)) {
			return ResponseEntity.badRequest().body(Map.of("error", "locale must be en, ja, or my"));
		}
		Long id = Long.valueOf(me.getUsername());
		UserEntity u = users.findById(id).orElse(null);
		if (u == null) {
			return ResponseEntity.notFound().build();
		}
		u.setLocale(lc);
		users.save(u);
		return ResponseEntity.ok(Map.of("locale", lc));
	}

	@PostMapping("/assistant/profile-tips")
	public ResponseEntity<?> profileTips(@AuthenticationPrincipal User me,
			@RequestBody(required = false) Map<String, Object> body) {
		String sub = me.getUsername();
		if (sub.startsWith("pending:")) {
			return ResponseEntity.status(403).body(Map.of("error", "Finish sign-up before using this."));
		}
		Long userId = Long.valueOf(sub);
		ProfileEntity profile = repo.findById(userId).orElseThrow();
		UserEntity user = users.findById(userId).orElseThrow();
		boolean llm = aiCoachService.meta().llmConfigured();
		String lang = user.getLocale();
		if (body != null && body.get("locale") instanceof String s && !s.isBlank()) {
			lang = s.trim();
		}
		if (lang == null || lang.isBlank()) {
			lang = "en";
		}
		var plan = subscriptionPlanService.resolve(profile);
		if (llm) {
			tieredAiUsageService.assertCanUse(userId, plan, AiFeature.PROFILE_AI);
		}
		int photoCount = (int) Math.min(Integer.MAX_VALUE, photoRepo.countByUserId(userId));
		var tips = aiCoachService.profileImprovementTips(lang, profile, photoCount, plan);
		if (llm) {
			tieredAiUsageService.recordUse(userId, AiFeature.PROFILE_AI);
		}
		AiQuotaView q = tieredAiUsageService.status(userId, plan, AiFeature.PROFILE_AI);
		return ResponseEntity.ok(Map.of(
				"tips", tips,
				"llmConfigured", llm,
				"subscriptionPlan", plan.name(),
				"aiQuota", Map.of(
						"usedToday", q.usedToday(),
						"dailyLimit", q.dailyLimit(),
						"remaining", q.remaining(),
						"fairUseCap", q.fairUseCap()),
				"aiEntitlements", tieredAiUsageService.entitlementsMap(userId, plan)));
	}

	@PutMapping("/profile")
	public ResponseEntity<?> upsert(@AuthenticationPrincipal User me, @RequestBody ProfileEntity patch) {
		Long id = Long.valueOf(me.getUsername());
		patch.setUserId(id);
		ProfileEntity existing = repo.findById(id).orElseThrow();
		applyProfilePatch(patch, existing);
		return ResponseEntity.ok(repo.save(existing));
	}

	/**
	 * Explicit discovery radius and optional coordinates. Use {@code maxDistanceKm: null} to show people
	 * at any distance (subject to deck limits).
	 */
	@PutMapping("/discovery-settings")
	public ResponseEntity<?> discoverySettings(@AuthenticationPrincipal User me,
			@RequestBody Map<String, Object> body) {
		Long id = Long.valueOf(me.getUsername());
		ProfileEntity p = repo.findById(id).orElseThrow();
		if (body.containsKey("maxDistanceKm")) {
			Object raw = body.get("maxDistanceKm");
			if (raw == null) {
				p.setDistanceKm(null);
			} else if (raw instanceof Number n) {
				int km = n.intValue();
				if (km <= 0)
					p.setDistanceKm(null);
				else
					p.setDistanceKm(Math.min(500, Math.max(1, km)));
			} else {
				return ResponseEntity.badRequest().body(Map.of("error", "maxDistanceKm must be a number or null"));
			}
		}
		if (body.containsKey("latitude") && body.get("latitude") instanceof Number)
			p.setLatitude(((Number) body.get("latitude")).doubleValue());
		if (body.containsKey("longitude") && body.get("longitude") instanceof Number)
			p.setLongitude(((Number) body.get("longitude")).doubleValue());
		if (body.containsKey("minAge")) {
			Object a = body.get("minAge");
			if (a == null)
				p.setMinAge(null);
			else if (a instanceof Number n)
				p.setMinAge(Math.max(18, Math.min(80, n.intValue())));
			else
				return ResponseEntity.badRequest().body(Map.of("error", "minAge must be a number or null"));
		}
		if (body.containsKey("maxAge")) {
			Object a = body.get("maxAge");
			if (a == null)
				p.setMaxAge(null);
			else if (a instanceof Number n)
				p.setMaxAge(Math.max(18, Math.min(80, n.intValue())));
			else
				return ResponseEntity.badRequest().body(Map.of("error", "maxAge must be a number or null"));
		}
		putJsonMap(body, "discoverySettings", p::setDiscoverySettings);
		putJsonMap(body, "lifestyle", p::setLifestyle);
		repo.save(p);
		Map<String, Object> out = new HashMap<>();
		out.put("maxDistanceKm", p.getDistanceKm());
		out.put("latitude", p.getLatitude());
		out.put("longitude", p.getLongitude());
		out.put("minAge", p.getMinAge());
		out.put("maxAge", p.getMaxAge());
		out.put("discoverySettings", p.getDiscoverySettings());
		out.put("lifestyle", p.getLifestyle());
		return ResponseEntity.ok(out);
	}

	@SuppressWarnings("unchecked")
	private static void putJsonMap(Map<String, Object> body, String key, Consumer<Map<String, Object>> setter) {
		if (!body.containsKey(key))
			return;
		Object raw = body.get(key);
		if (raw == null) {
			setter.accept(null);
			return;
		}
		if (raw instanceof Map<?, ?> m) {
			Map<String, Object> out = new HashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
			setter.accept(out);
		}
	}

	/** Merge non-null fields from patch so JSON partial updates do not null out stored data. */
	private static void applyProfilePatch(ProfileEntity patch, ProfileEntity target) {
		if (patch.getDisplayName() != null)
			target.setDisplayName(patch.getDisplayName());
		if (patch.getBio() != null)
			target.setBio(patch.getBio());
		if (patch.getGender() != null)
			target.setGender(patch.getGender());
		if (patch.getBirthday() != null)
			target.setBirthday(patch.getBirthday());
		if (patch.getLatitude() != null)
			target.setLatitude(patch.getLatitude());
		if (patch.getLongitude() != null)
			target.setLongitude(patch.getLongitude());
		if (patch.getMinAge() != null)
			target.setMinAge(patch.getMinAge());
		if (patch.getMaxAge() != null)
			target.setMaxAge(patch.getMaxAge());
		if (patch.getDistanceKm() != null)
			target.setDistanceKm(patch.getDistanceKm());
		if (patch.getCity() != null)
			target.setCity(patch.getCity());
		if (patch.getEducation() != null)
			target.setEducation(patch.getEducation());
		if (patch.getOccupation() != null)
			target.setOccupation(patch.getOccupation());
		if (patch.getHobbies() != null)
			target.setHobbies(patch.getHobbies());
		if (patch.getInterests() != null)
			target.setInterests(patch.getInterests());
		if (patch.getPhotoUrl() != null)
			target.setPhotoUrl(patch.getPhotoUrl());
		if (patch.getDiscoverySettings() != null)
			target.setDiscoverySettings(patch.getDiscoverySettings());
		if (patch.getLifestyle() != null)
			target.setLifestyle(patch.getLifestyle());
	}
	@PostMapping("/profile")
	public ResponseEntity<?> create(
	        @AuthenticationPrincipal User me,
	        @RequestBody ProfileEntity body) {

	    // prevent overwriting existing profile
	    if (repo.existsById(Long.valueOf(me.getUsername()))) {
	        return ResponseEntity.badRequest().body("Profile already exists");
	    }

	    body.setUserId(Long.valueOf(me.getUsername()));
	    return ResponseEntity.ok(repo.save(body));
	}

	public static class UpgradeBody {
		/** Optional: PLUS (default) or GOLD — demo billing hook. */
		public String plan;
	}

	@PostMapping("/upgrade")
	public ResponseEntity<?> upgrade(@AuthenticationPrincipal User me, @RequestBody(required = false) UpgradeBody body) {
		Long id = Long.valueOf(me.getUsername());
		ProfileEntity p = repo.findById(id).orElse(null);
		if (p == null)
			return ResponseEntity.notFound().build();
		com.example.dating.model.subscription.SubscriptionPlan target = com.example.dating.model.subscription.SubscriptionPlan.PLUS;
		if (body != null && body.plan != null && !body.plan.isBlank()) {
			try {
				target = com.example.dating.model.subscription.SubscriptionPlan
						.valueOf(body.plan.trim().toUpperCase(java.util.Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().body(Map.of("error", "plan must be PLUS or GOLD"));
			}
		}
		userSubscriptionService.applyDevDemoPlan(id, target);
		ProfileEntity refreshed = repo.findById(id).orElse(p);
		return ResponseEntity.ok(Map.of(
				"isPremium", refreshed.isPremium(),
				"subscriptionPlan", target.name()));
	}

}
