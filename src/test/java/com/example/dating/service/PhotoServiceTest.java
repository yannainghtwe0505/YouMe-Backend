package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhotoServiceTest {

	@Mock
	private PhotoRepo photoRepo;

	@Mock
	private ProfileRepo profileRepo;

	@Mock
	private MediaUrls mediaUrls;

	@Mock
	private S3ObjectDeleteService s3Delete;

	@InjectMocks
	private PhotoService photoService;

	@Test
	void deleteOwnedPhoto_returnsFalse_whenNotOwner() {
		when(photoRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());
		assertFalse(photoService.deleteOwnedPhoto(1L, 9L));
		verify(photoRepo, never()).delete(any());
	}

	@Test
	void deleteOwnedPhoto_deletesAndClearsPrimary_whenWasPrimaryAndNoRemainingPhotos() {
		PhotoEntity ph = new PhotoEntity();
		ph.setId(50L);
		ph.setUserId(200L);
		ph.setS3Key("uploads/200/a.jpg");
		ph.setPrimaryPhoto(true);
		when(photoRepo.findByIdAndUserId(50L, 200L)).thenReturn(Optional.of(ph));
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(200L)).thenReturn(List.of());
		ProfileEntity profile = new ProfileEntity();
		profile.setUserId(200L);
		profile.setPhotoUrl("https://old");
		when(profileRepo.findById(200L)).thenReturn(Optional.of(profile));

		assertTrue(photoService.deleteOwnedPhoto(200L, 50L));
		verify(photoRepo).delete(ph);
		verify(s3Delete).deleteKeyBestEffort("uploads/200/a.jpg");
		verify(profileRepo).save(profile);
		assertNull(profile.getPhotoUrl());
	}

	@Test
	void setPrimaryPhoto_updatesFlagsAndProfileUrl() {
		PhotoEntity target = new PhotoEntity();
		target.setId(1L);
		target.setUserId(10L);
		target.setS3Key("k-target");
		target.setPrimaryPhoto(false);
		PhotoEntity other = new PhotoEntity();
		other.setId(2L);
		other.setUserId(10L);
		other.setS3Key("k-other");
		other.setPrimaryPhoto(true);
		when(photoRepo.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(target));
		List<PhotoEntity> mutable = new ArrayList<>(List.of(target, other));
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(10L)).thenReturn(mutable);
		ProfileEntity profile = new ProfileEntity();
		profile.setUserId(10L);
		when(profileRepo.findById(10L)).thenReturn(Optional.of(profile));
		when(mediaUrls.urlForKey("k-target")).thenReturn("https://cdn/target");

		assertTrue(photoService.setPrimaryPhoto(10L, 1L));
		verify(photoRepo).saveAll(mutable);
		assertTrue(Boolean.TRUE.equals(target.getPrimaryPhoto()));
		assertFalse(Boolean.TRUE.equals(other.getPrimaryPhoto()));
		assertEquals("https://cdn/target", profile.getPhotoUrl());
	}

	@Test
	void setPrimaryPhoto_returnsFalse_whenWrongUser() {
		when(photoRepo.findByIdAndUserId(3L, 99L)).thenReturn(Optional.empty());
		assertFalse(photoService.setPrimaryPhoto(99L, 3L));
	}
}