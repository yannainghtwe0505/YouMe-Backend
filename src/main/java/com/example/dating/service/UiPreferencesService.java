package com.example.dating.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.UserRepo;

@Service
public class UiPreferencesService {
	public static final int ONBOARDING_VERSION = 1;

	private final UserRepo users;

	public UiPreferencesService(UserRepo users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getPreferences(long userId) {
		UserEntity u = users.findById(userId).orElseThrow();
		return normalize(copyMap(u.getUiPreferences()));
	}

	@Transactional
	public Map<String, Object> patchPreferences(long userId, Map<String, Object> patch) {
		UserEntity u = users.findById(userId).orElseThrow();
		Map<String, Object> prefs = normalize(copyMap(u.getUiPreferences()));
		if (patch == null || patch.isEmpty()) {
			return prefs;
		}
		mergePatch(prefs, patch);
		u.setUiPreferences(prefs);
		users.save(u);
		return prefs;
	}

	@Transactional
	public Map<String, Object> dismissTooltip(long userId, String tooltipId) {
		if (tooltipId == null || tooltipId.isBlank()) {
			return getPreferences(userId);
		}
		Map<String, Object> patch = Map.of(
				"tooltipsDismissed", List.of(tooltipId.trim()));
		return patchPreferences(userId, patch);
	}

	@Transactional
	public Map<String, Object> completeOnboarding(long userId, boolean skipped) {
		Map<String, Object> patch = new HashMap<>();
		patch.put("onboardingVersion", ONBOARDING_VERSION);
		if (skipped) {
			patch.put("onboardingSkippedAt", Instant.now().toString());
		} else {
			patch.put("onboardingCompletedAt", Instant.now().toString());
		}
		return patchPreferences(userId, patch);
	}

	@SuppressWarnings("unchecked")
	private static void mergePatch(Map<String, Object> prefs, Map<String, Object> patch) {
		for (Map.Entry<String, Object> e : patch.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			if ("tooltipsDismissed".equals(key) && val instanceof List<?> list) {
				Set<String> merged = new HashSet<>(stringList(prefs.get("tooltipsDismissed")));
				for (Object o : list) {
					if (o != null && !String.valueOf(o).isBlank()) {
						merged.add(String.valueOf(o).trim());
					}
				}
				prefs.put("tooltipsDismissed", new ArrayList<>(merged));
			} else if (val != null) {
				prefs.put(key, val);
			}
		}
	}

	private static Map<String, Object> normalize(Map<String, Object> prefs) {
		if (!prefs.containsKey("onboardingVersion")) {
			prefs.put("onboardingVersion", 0);
		}
		if (!prefs.containsKey("tooltipsDismissed")) {
			prefs.put("tooltipsDismissed", List.of());
		}
		return prefs;
	}

	@SuppressWarnings("unchecked")
	private static List<String> stringList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object o : list) {
			if (o != null && !String.valueOf(o).isBlank()) {
				out.add(String.valueOf(o).trim());
			}
		}
		return out;
	}

	private static Map<String, Object> copyMap(Map<String, Object> src) {
		return src == null ? new HashMap<>() : new HashMap<>(src);
	}
}
