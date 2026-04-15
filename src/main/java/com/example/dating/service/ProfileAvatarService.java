package com.example.dating.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.PhotoRepo;

/**
 * Resolves a user's display avatar URL the same way as {@code GET /me}: prefer {@code photos.s3_key} with
 * {@link MediaUrls#urlForKey(String)} so presigned URLs are regenerated. {@code profiles.photo_url} often stores
 * a single presigned URL that expires, which breaks list endpoints that only read that column.
 */
@Service
public class ProfileAvatarService {

	private final PhotoRepo photoRepo;
	private final MediaUrls mediaUrls;

	public ProfileAvatarService(PhotoRepo photoRepo, MediaUrls mediaUrls) {
		this.photoRepo = photoRepo;
		this.mediaUrls = mediaUrls;
	}

	public String resolveAvatarUrl(Long userId, ProfileEntity profile) {
		if (userId == null)
			return null;
		List<PhotoEntity> photos = photoRepo.findByUserIdOrderByCreatedAtAsc(userId);
		if (!photos.isEmpty()) {
			Optional<PhotoEntity> primary = photos.stream()
					.filter(p -> Boolean.TRUE.equals(p.getPrimaryPhoto()))
					.findFirst();
			PhotoEntity pick = primary.orElse(photos.get(0));
			return mediaUrls.urlForKey(pick.getS3Key());
		}
		if (profile == null)
			return null;
		String raw = profile.getPhotoUrl();
		if (raw == null || raw.isBlank())
			return null;
		String t = raw.strip();
		/* Bare S3 object key stored in legacy rows */
		if (!t.startsWith("http://") && !t.startsWith("https://"))
			return mediaUrls.urlForKey(t);
		/* Full URL: public bucket or legacy; may be an expired presigned URL — cannot refresh without key */
		return t;
	}
}
