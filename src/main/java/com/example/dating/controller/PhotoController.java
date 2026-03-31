package com.example.dating.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.PresignService;

@RestController
@RequestMapping("/photos")
public class PhotoController {
	private final PresignService presign;
	private final PhotoRepo photoRepo;
	private final ProfileRepo profileRepo;

	@Value("${app.media.public-base-url:https://example-s3}")
	private String publicBaseUrl;

	public PhotoController(PresignService presign, PhotoRepo photoRepo, ProfileRepo profileRepo) {
		this.presign = presign;
		this.photoRepo = photoRepo;
		this.profileRepo = profileRepo;
	}

	@PostMapping("/presign")
	public ResponseEntity<?> presign(@AuthenticationPrincipal User me, @RequestParam String filename,
			@RequestParam String contentType) {
		Long userId = Long.valueOf(me.getUsername());
		var result = presign.presignUpload(userId, filename, contentType);
		return ResponseEntity.ok(Map.of(
				"uploadUrl", result.uploadUrl().toString(),
				"s3Key", result.s3Key()));
	}

	@PostMapping("/complete")
	public ResponseEntity<?> complete(@AuthenticationPrincipal User me, @RequestParam String s3Key) {
		Long userId = Long.valueOf(me.getUsername());
		PhotoEntity ph = new PhotoEntity();
		ph.setUserId(userId);
		ph.setS3Key(s3Key);
		boolean first = photoRepo.countByUserId(userId) == 0;
		ph.setPrimaryPhoto(first);
		photoRepo.save(ph);
		if (first) {
			profileRepo.findById(userId).ifPresent(p -> {
				String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
						: publicBaseUrl;
				p.setPhotoUrl(base + "/" + s3Key);
				profileRepo.save(p);
			});
		}
		return ResponseEntity.ok(Map.of("id", ph.getId(), "s3Key", s3Key));
	}
}
