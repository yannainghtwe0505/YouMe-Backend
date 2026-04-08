package com.example.dating.dto;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.dating.model.entity.PendingRegistrationEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserEntity;

public record MeResponse(
		Long userId,
		String email,
		String phoneE164,
		boolean registrationComplete,
		String onboardingStep,
		String name,
		String bio,
		Integer age,
		String location,
		Integer distanceKm,
		String distance,
		String education,
		String work,
		String hobby,
		boolean isPremium,
		String avatar,
		Double latitude,
		Double longitude,
		List<String> photos,
		Map<String, Object> discoverySettings,
		Map<String, Object> lifestyle,
		Integer minAge,
		Integer maxAge) {

	public static MeResponse from(UserEntity user, ProfileEntity p, List<String> photoUrls) {
		Integer age = null;
		if (p.getBirthday() != null)
			age = Period.between(p.getBirthday(), LocalDate.now()).getYears();
		String dist = p.getDistanceKm() != null ? String.valueOf(p.getDistanceKm()) : null;
		List<String> photos = photoUrls == null ? List.of() : List.copyOf(photoUrls);
		return new MeResponse(
				user.getId(),
				user.getEmail(),
				user.getPhoneE164(),
				user.isRegistrationComplete(),
				user.getOnboardingStep(),
				p.getDisplayName(),
				p.getBio(),
				age,
				p.getCity(),
				p.getDistanceKm(),
				dist,
				p.getEducation(),
				p.getOccupation(),
				p.getHobbies(),
				p.isPremium(),
				p.getPhotoUrl(),
				p.getLatitude(),
				p.getLongitude(),
				photos,
				p.getDiscoverySettings(),
				p.getLifestyle(),
				p.getMinAge(),
				p.getMaxAge());
	}

	public static MeResponse fromPending(PendingRegistrationEntity row, Map<String, Object> draft) {
		String displayName = null;
		LocalDate birthday = null;
		String city = null;
		Map<String, Object> lifestyle = null;
		if (draft != null && !draft.isEmpty()) {
			Object dn = draft.get("displayName");
			if (dn != null && !String.valueOf(dn).isBlank()) {
				displayName = String.valueOf(dn).trim();
			}
			Object bd = draft.get("birthday");
			if (bd != null && !String.valueOf(bd).isBlank()) {
				try {
					birthday = LocalDate.parse(String.valueOf(bd).trim());
				} catch (Exception ignored) {
					/* ignore malformed draft */
				}
			}
			Object c = draft.get("city");
			if (c != null && !String.valueOf(c).isBlank()) {
				city = String.valueOf(c).trim();
			}
			Object life = draft.get("lifestyle");
			if (life instanceof Map<?, ?> m) {
				Map<String, Object> lm = new HashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					lm.put(String.valueOf(e.getKey()), e.getValue());
				}
				lifestyle = lm.isEmpty() ? null : lm;
			}
		}
		Integer age = birthday != null ? Period.between(birthday, LocalDate.now()).getYears() : null;
		return new MeResponse(
				null,
				row.getEmail(),
				row.getPhoneE164(),
				false,
				row.getOnboardingStep(),
				displayName,
				null,
				age,
				city,
				null,
				null,
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				List.of(),
				null,
				lifestyle,
				null,
				null);
	}
}
