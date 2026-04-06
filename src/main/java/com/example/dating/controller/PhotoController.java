package com.example.dating.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.PhotoService;
import com.example.dating.service.PresignService;

@RestController
@RequestMapping("/photos")
public class PhotoController {
	public static final int MAX_PHOTOS_PER_USER = 6;

	private final PresignService presign;
	private final PhotoRepo photoRepo;
	private final ProfileRepo profileRepo;
	private final MediaUrls mediaUrls;
	private final PhotoService photoService;

	public PhotoController(PresignService presign, PhotoRepo photoRepo, ProfileRepo profileRepo, MediaUrls mediaUrls,
			PhotoService photoService) {
		this.presign = presign;
		this.photoRepo = photoRepo;
		this.profileRepo = profileRepo;
		this.mediaUrls = mediaUrls;
		this.photoService = photoService;
	}

	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> listMine(@AuthenticationPrincipal User me) {
		Long userId = Long.valueOf(me.getUsername());
		List<Map<String, Object>> out = photoRepo.findByUserIdOrderByCreatedAtAsc(userId).stream()
				.map(ph -> Map.<String, Object>of(
						"id", ph.getId(),
						"url", mediaUrls.urlForKey(ph.getS3Key()),
						"primary", Boolean.TRUE.equals(ph.getPrimaryPhoto())))
				.toList();
		return ResponseEntity.ok(out);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteMine(@AuthenticationPrincipal User me, @PathVariable("id") Long photoId) {
		Long userId = Long.valueOf(me.getUsername());
		return photoService.deleteOwnedPhoto(userId, photoId) ? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}

	@PutMapping("/{id}/primary")
	public ResponseEntity<?> setPrimary(@AuthenticationPrincipal User me, @PathVariable("id") Long photoId) {
		Long userId = Long.valueOf(me.getUsername());
		return photoService.setPrimaryPhoto(userId, photoId) ? ResponseEntity.ok(Map.of("ok", true))
				: ResponseEntity.notFound().build();
	}

	@PostMapping("/presign")
	public ResponseEntity<?> presign(@AuthenticationPrincipal User me, @RequestParam String filename,
			@RequestParam String contentType) {
		Long userId = Long.valueOf(me.getUsername());
		if (photoRepo.countByUserId(userId) >= MAX_PHOTOS_PER_USER) {
			return ResponseEntity.badRequest().body(Map.of("error", "Maximum " + MAX_PHOTOS_PER_USER + " photos"));
		}
		var result = presign.presignUpload(userId, filename, contentType);
		return ResponseEntity.ok(Map.of(
				"uploadUrl", result.uploadUrl().toString(),
				"s3Key", result.s3Key()));
	}

	@PostMapping("/complete")
	public ResponseEntity<?> complete(@AuthenticationPrincipal User me, @RequestParam String s3Key) {
		Long userId = Long.valueOf(me.getUsername());
		if (photoRepo.countByUserId(userId) >= MAX_PHOTOS_PER_USER) {
			return ResponseEntity.badRequest().body(Map.of("error", "Maximum " + MAX_PHOTOS_PER_USER + " photos"));
		}
		PhotoEntity ph = new PhotoEntity();
		ph.setUserId(userId);
		ph.setS3Key(s3Key);
		boolean first = photoRepo.countByUserId(userId) == 0;
		ph.setPrimaryPhoto(first);
		photoRepo.save(ph);
		if (first) {
			profileRepo.findById(userId).ifPresent(p -> {
				String url = mediaUrls.urlForKey(s3Key);
				if (url != null) {
					p.setPhotoUrl(url);
					profileRepo.save(p);
				}
			});
		}
		return ResponseEntity.ok(Map.of("id", ph.getId(), "s3Key", s3Key));
	}
}
