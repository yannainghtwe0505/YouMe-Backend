package com.example.dating.dto;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserEntity;

public record MeResponse(
		Long userId,
		String email,
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
		List<String> photos) {

	public static MeResponse from(UserEntity user, ProfileEntity p, List<String> photoUrls) {
		Integer age = null;
		if (p.getBirthday() != null)
			age = Period.between(p.getBirthday(), LocalDate.now()).getYears();
		String dist = p.getDistanceKm() != null ? String.valueOf(p.getDistanceKm()) : null;
		List<String> photos = photoUrls == null ? List.of() : List.copyOf(photoUrls);
		return new MeResponse(
				user.getId(),
				user.getEmail(),
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
				photos);
	}
}
