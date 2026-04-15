package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.PhotoRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileAvatarServiceTest {

	@Mock
	private PhotoRepo photoRepo;

	@Mock
	private MediaUrls mediaUrls;

	@InjectMocks
	private ProfileAvatarService profileAvatarService;

	@Test
	void resolveAvatarUrl_returnsNull_whenUserIdNull() {
		assertNull(profileAvatarService.resolveAvatarUrl(null, new ProfileEntity()));
	}

	@Test
	void resolveAvatarUrl_prefersPrimaryPhotoKey() {
		long userId = 42L;
		PhotoEntity primary = new PhotoEntity();
		primary.setS3Key("uploads/42/p.jpg");
		primary.setPrimaryPhoto(true);
		PhotoEntity other = new PhotoEntity();
		other.setS3Key("uploads/42/q.jpg");
		other.setPrimaryPhoto(false);
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(other, primary));
		when(mediaUrls.urlForKey("uploads/42/p.jpg")).thenReturn("https://signed.example/p.jpg");
		assertEquals("https://signed.example/p.jpg", profileAvatarService.resolveAvatarUrl(userId, null));
		verify(mediaUrls).urlForKey("uploads/42/p.jpg");
	}

	@Test
	void resolveAvatarUrl_fallsBackToFirstPhoto_whenNoPrimary() {
		long userId = 7L;
		PhotoEntity a = new PhotoEntity();
		a.setS3Key("k1");
		a.setPrimaryPhoto(false);
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(a));
		when(mediaUrls.urlForKey("k1")).thenReturn("https://u1");
		assertEquals("https://u1", profileAvatarService.resolveAvatarUrl(userId, null));
	}

	@Test
	void resolveAvatarUrl_legacyBareKeyInProfileColumn() {
		long userId = 9L;
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
		ProfileEntity p = new ProfileEntity();
		p.setPhotoUrl("uploads/9/legacy-key");
		when(mediaUrls.urlForKey("uploads/9/legacy-key")).thenReturn("https://wrapped");
		assertEquals("https://wrapped", profileAvatarService.resolveAvatarUrl(userId, p));
	}

	@Test
	void resolveAvatarUrl_returnsFullHttpUrlUntouched() {
		long userId = 3L;
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
		ProfileEntity p = new ProfileEntity();
		p.setPhotoUrl("https://bucket.example/old-presigned");
		assertEquals("https://bucket.example/old-presigned", profileAvatarService.resolveAvatarUrl(userId, p));
	}

	@Test
	void resolveAvatarUrl_blankPhotoUrl_returnsNull() {
		long userId = 1L;
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
		ProfileEntity p = new ProfileEntity();
		p.setPhotoUrl("   ");
		assertNull(profileAvatarService.resolveAvatarUrl(userId, p));
	}
}