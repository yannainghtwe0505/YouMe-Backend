package com.example.dating.dto;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
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
		Integer maxAge,
		String gender,
		LocalDate birthday,
		List<String> interests,
		String locale,
		AiQuotaView aiQuota,
		String subscriptionPlan,
		Map<String, Object> aiEntitlements) {

	public static MeResponse from(UserEntity user, ProfileEntity p, List<String> photoUrls, String resolvedAvatar,
			AiQuotaView aiQuota, String subscriptionPlan, Map<String, Object> aiEntitlements) {
		Integer age = null;
		if (p.getBirthday() != null)
			age = Period.between(p.getBirthday(), LocalDate.now()).getYears();
		String dist = p.getDistanceKm() != null ? String.valueOf(p.getDistanceKm()) : null;
		List<String> photos = photoUrls == null ? List.of() : List.copyOf(photoUrls);
		List<String> interests = p.getInterests() == null ? List.of() : List.copyOf(p.getInterests());
		String loc = user.getLocale();
		if (loc == null || loc.isBlank())
			loc = "en";
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
				resolvedAvatar != null ? resolvedAvatar : p.getPhotoUrl(),
				p.getLatitude(),
				p.getLongitude(),
				photos,
				p.getDiscoverySettings(),
				p.getLifestyle(),
				p.getMinAge(),
				p.getMaxAge(),
				p.getGender(),
				p.getBirthday(),
				interests,
				loc,
				aiQuota,
				subscriptionPlan,
				aiEntitlements);
	}

	public static MeResponse fromPending(PendingRegistrationEntity row, Map<String, Object> draft) {
		String displayName = null;
		String bio = null;
		String gender = null;
		LocalDate birthday = null;
		String city = null;
		List<String> interests = null;
		Map<String, Object> lifestyle = null;
		if (draft != null && !draft.isEmpty()) {
			Object dn = draft.get("displayName");
			if (dn != null && !String.valueOf(dn).isBlank()) {
				displayName = String.valueOf(dn).trim();
			}
			Object bioRaw = draft.get("bio");
			if (bioRaw != null && !String.valueOf(bioRaw).isBlank()) {
				bio = String.valueOf(bioRaw).trim();
			}
			Object g = draft.get("gender");
			if (g != null && !String.valueOf(g).isBlank()) {
				gender = String.valueOf(g).trim();
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
			Object intr = draft.get("interests");
			if (intr instanceof List<?> list) {
				List<String> cleaned = new ArrayList<>();
				for (Object o : list) {
					if (o != null && !String.valueOf(o).isBlank())
						cleaned.add(String.valueOf(o).trim());
				}
				if (!cleaned.isEmpty())
					interests = cleaned;
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
		List<String> interestList = interests == null ? List.of() : List.copyOf(interests);
		return new MeResponse(
				null,
				row.getEmail(),
				row.getPhoneE164(),
				false,
				row.getOnboardingStep(),
				displayName,
				bio,
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
				null,
				gender,
				birthday,
				interestList,
				null,
				null,
				"FREE",
				Map.of());
	}
}
