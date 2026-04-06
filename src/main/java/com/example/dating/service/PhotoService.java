package com.example.dating.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;

@Service
public class PhotoService {
	private final PhotoRepo photoRepo;
	private final ProfileRepo profileRepo;
	private final MediaUrls mediaUrls;
	private final S3ObjectDeleteService s3Delete;

	public PhotoService(PhotoRepo photoRepo, ProfileRepo profileRepo, MediaUrls mediaUrls,
			S3ObjectDeleteService s3Delete) {
		this.photoRepo = photoRepo;
		this.profileRepo = profileRepo;
		this.mediaUrls = mediaUrls;
		this.s3Delete = s3Delete;
	}

	@Transactional
	public boolean deleteOwnedPhoto(Long userId, Long photoId) {
		return photoRepo.findByIdAndUserId(photoId, userId).map(ph -> {
			boolean wasPrimary = Boolean.TRUE.equals(ph.getPrimaryPhoto());
			String key = ph.getS3Key();
			photoRepo.delete(ph);
			s3Delete.deleteKeyBestEffort(key);
			if (wasPrimary) {
				List<PhotoEntity> rest = photoRepo.findByUserIdOrderByCreatedAtAsc(userId);
				profileRepo.findById(userId).ifPresent(profile -> {
					if (rest.isEmpty()) {
						profile.setPhotoUrl(null);
					} else {
						PhotoEntity next = rest.get(0);
						next.setPrimaryPhoto(true);
						photoRepo.save(next);
						String url = mediaUrls.urlForKey(next.getS3Key());
						profile.setPhotoUrl(url);
					}
					profileRepo.save(profile);
				});
			}
			return true;
		}).orElse(false);
	}

	@Transactional
	public boolean setPrimaryPhoto(Long userId, Long photoId) {
		return photoRepo.findByIdAndUserId(photoId, userId).map(target -> {
			List<PhotoEntity> all = photoRepo.findByUserIdOrderByCreatedAtAsc(userId);
			for (PhotoEntity p : all) {
				p.setPrimaryPhoto(p.getId().equals(target.getId()));
			}
			photoRepo.saveAll(all);
			profileRepo.findById(userId).ifPresent(profile -> {
				profile.setPhotoUrl(mediaUrls.urlForKey(target.getS3Key()));
				profileRepo.save(profile);
			});
			return true;
		}).orElse(false);
	}
}
