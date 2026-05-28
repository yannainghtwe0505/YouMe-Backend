package com.example.dating.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.UserDeviceTokenEntity;
import com.example.dating.repository.UserDeviceTokenRepo;

@Service
public class NotificationPreferenceService {
	private final UserDeviceTokenRepo userDeviceTokenRepo;

	public NotificationPreferenceService(UserDeviceTokenRepo userDeviceTokenRepo) {
		this.userDeviceTokenRepo = userDeviceTokenRepo;
	}

	@Transactional
	public void registerOrUpdateToken(Long userId, String token, String platform, String locale, boolean enabled) {
		String cleanedToken = normalizeToken(token);
		String cleanedPlatform = normalizePlatform(platform);
		String cleanedLocale = normalizeLocale(locale);
		UserDeviceTokenEntity row = userDeviceTokenRepo.findByToken(cleanedToken).orElseGet(UserDeviceTokenEntity::new);
		row.setUserId(userId);
		row.setToken(cleanedToken);
		row.setPlatform(cleanedPlatform);
		row.setLocale(cleanedLocale);
		row.setEnabled(enabled);
		row.setLastSeenAt(Instant.now());
		userDeviceTokenRepo.save(row);
	}

	@Transactional
	public boolean removeToken(Long userId, String token) {
		String cleanedToken = normalizeToken(token);
		UserDeviceTokenEntity row = userDeviceTokenRepo.findByToken(cleanedToken).orElse(null);
		if (row == null || !userId.equals(row.getUserId())) {
			return false;
		}
		userDeviceTokenRepo.delete(row);
		return true;
	}

	@Transactional
	public void setEnabledForUser(Long userId, boolean enabled) {
		List<UserDeviceTokenEntity> tokens = userDeviceTokenRepo.findByUserId(userId);
		Instant now = Instant.now();
		for (UserDeviceTokenEntity token : tokens) {
			token.setEnabled(enabled);
			token.setLastSeenAt(now);
		}
		userDeviceTokenRepo.saveAll(tokens);
	}

	public Map<String, Object> summary(Long userId) {
		List<UserDeviceTokenEntity> tokens = userDeviceTokenRepo.findByUserId(userId);
		long total = tokens.size();
		long enabled = tokens.stream().filter(UserDeviceTokenEntity::isEnabled).count();
		return Map.of(
				"pushEnabled", enabled > 0,
				"registeredDevices", total);
	}

	private static String normalizeToken(String token) {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("token is required");
		}
		String out = token.trim();
		if (out.length() > 8000) {
			throw new IllegalArgumentException("token too long");
		}
		return out;
	}

	private static String normalizePlatform(String platform) {
		String out = platform == null ? "WEB" : platform.trim().toUpperCase(Locale.ROOT);
		if (out.isBlank()) {
			out = "WEB";
		}
		if (out.length() > 24) {
			out = out.substring(0, 24);
		}
		return out;
	}

	private static String normalizeLocale(String locale) {
		if (locale == null || locale.isBlank()) {
			return null;
		}
		String out = locale.trim().toLowerCase(Locale.ROOT);
		if (out.length() > 12) {
			out = out.substring(0, 12);
		}
		return out;
	}
}
