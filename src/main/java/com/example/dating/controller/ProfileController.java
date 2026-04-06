package com.example.dating.controller;

import java.util.Map;

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
import com.example.dating.dto.ChangePasswordRequest;
import com.example.dating.dto.MeResponse;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;

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

	public ProfileController(ProfileRepo repo, UserRepo users, PhotoRepo photoRepo, MediaUrls mediaUrls,
			PasswordEncoder passwordEncoder) {
		this.repo = repo;
		this.users = users;
		this.photoRepo = photoRepo;
		this.mediaUrls = mediaUrls;
		this.passwordEncoder = passwordEncoder;
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
		Long id = Long.valueOf(me.getUsername());
		if (!repo.existsById(id))
			return ResponseEntity.notFound().build();
		var user = users.findById(id).orElseThrow();
		var profile = repo.findById(id).orElseThrow();
		List<String> photoUrls = photoRepo.findByUserIdOrderByCreatedAtAsc(id).stream()
				.map(ph -> mediaUrls.urlForKey(ph.getS3Key()))
				.filter(u -> u != null)
				.limit(6)
				.toList();
		return ResponseEntity.ok(MeResponse.from(user, profile, photoUrls));
	}

	@PutMapping("/profile")
	public ResponseEntity<?> upsert(@AuthenticationPrincipal User me, @RequestBody ProfileEntity patch) {
		Long id = Long.valueOf(me.getUsername());
		patch.setUserId(id);
		ProfileEntity existing = repo.findById(id).orElseThrow();
		applyProfilePatch(patch, existing);
		return ResponseEntity.ok(repo.save(existing));
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

	@PostMapping("/upgrade")
	public ResponseEntity<?> upgrade(@AuthenticationPrincipal User me) {
		Long id = Long.valueOf(me.getUsername());
		return repo.findById(id).map(p -> {
			p.setPremium(true);
			repo.save(p);
			return ResponseEntity.ok(Map.of("isPremium", true));
		}).orElse(ResponseEntity.notFound().build());
	}

}
