package com.example.dating.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.security.JwtService;

@Service
public class AccountRegistrationService {

	public static final int MAX_PHOTOS = 6;
	public static final long MAX_BYTES_PER_PHOTO = 5 * 1024 * 1024;

	private final UserRepo users;
	private final ProfileRepo profiles;
	private final PhotoRepo photos;
	private final PasswordEncoder encoder;
	private final JwtService jwt;
	private final S3ObjectPutService s3Put;
	private final MediaUrls mediaUrls;

	public AccountRegistrationService(UserRepo users, ProfileRepo profiles, PhotoRepo photos, PasswordEncoder encoder,
			JwtService jwt, S3ObjectPutService s3Put, MediaUrls mediaUrls) {
		this.users = users;
		this.profiles = profiles;
		this.photos = photos;
		this.encoder = encoder;
		this.jwt = jwt;
		this.s3Put = s3Put;
		this.mediaUrls = mediaUrls;
	}

	public boolean isS3Available() {
		return s3Put.isAvailable();
	}

	public record RegisterWithProfileResult(String token, Long userId) {
	}

	@Transactional
	public RegisterWithProfileResult registerWithProfile(String email, String password, String displayName,
			MultipartFile[] photoParts) {
		if (!s3Put.isAvailable()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Photo storage is not available. Configure AWS S3 credentials to sign up with photos.");
		}
		String em = email == null ? "" : email.trim();
		String pw = password == null ? "" : password;
		String name = displayName == null ? "" : displayName.trim();
		if (em.isEmpty() || !em.contains("@")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required");
		}
		if (pw.length() < 6) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
		}
		if (name.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
		}
		if (name.length() > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is too long");
		}
		MultipartFile[] files = photoParts == null ? new MultipartFile[0] : photoParts;
		if (files.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one photo is required");
		}
		if (files.length > MAX_PHOTOS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most " + MAX_PHOTOS + " photos allowed");
		}
		for (MultipartFile f : files) {
			validatePhotoFile(f);
		}
		if (users.findByEmail(em).isPresent()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email in use");
		}

		UserEntity u = new UserEntity();
		u.setEmail(em);
		u.setPasswordHash(encoder.encode(pw));
		u.setRegistrationComplete(true);
		u.setOnboardingStep("DONE");
		u = users.save(u);
		Long userId = u.getId();

		ProfileEntity profile = new ProfileEntity();
		profile.setUserId(userId);
		profile.setDisplayName(name);
		profiles.save(profile);

		savePhotosForUser(userId, files, profile);

		String token = jwt.generate(u.getId(), u.getEmail());
		return new RegisterWithProfileResult(token, u.getId());
	}

	private static void validatePhotoFile(MultipartFile f) {
		if (f == null || f.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each photo must be a non-empty image file");
		}
		String ct = f.getContentType();
		if (ct == null || !ct.toLowerCase().startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photos must be image files (e.g. JPEG, PNG)");
		}
	}

	private static String buildObjectKey(long userId, String filename) {
		String safe = filename == null || filename.isBlank() ? "photo.jpg" : filename;
		safe = safe.replaceAll("[^a-zA-Z0-9._-]", "_");
		return "uploads/" + userId + "/" + UUID.randomUUID() + "_" + safe;
	}

	/**
	 * Upload profile images for an existing user and set primary + profile.photoUrl.
	 */
	@Transactional
	public void savePhotosForUser(long userId, MultipartFile[] photoParts, ProfileEntity profile) {
		MultipartFile[] files = photoParts == null ? new MultipartFile[0] : photoParts;
		if (files.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one photo is required");
		}
		if (files.length > MAX_PHOTOS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most " + MAX_PHOTOS + " photos allowed");
		}
		for (MultipartFile f : files) {
			validatePhotoFile(f);
		}
		boolean first = true;
		for (MultipartFile file : files) {
			byte[] bytes;
			try {
				bytes = file.getBytes();
			} catch (IOException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read photo upload");
			}
			if (bytes.length == 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty photo file");
			}
			if (bytes.length > MAX_BYTES_PER_PHOTO) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each photo must be at most 5 MB");
			}
			String key = buildObjectKey(userId, file.getOriginalFilename());
			String ct = file.getContentType();
			s3Put.putObject(key, bytes, ct);
			PhotoEntity ph = new PhotoEntity();
			ph.setUserId(userId);
			ph.setS3Key(key);
			ph.setPrimaryPhoto(first);
			photos.save(ph);
			if (first) {
				String url = mediaUrls.urlForKey(key);
				if (url != null) {
					profile.setPhotoUrl(url);
					profiles.save(profile);
				}
				first = false;
			}
		}
	}
}
