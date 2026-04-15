package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.PassRepo;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedServiceTest {

	@Mock
	private ProfileRepo profiles;

	@Mock
	private LikeRepo likeRepo;

	@Mock
	private PassRepo passRepo;

	@Mock
	private PhotoRepo photoRepo;

	@Mock
	private MediaUrls mediaUrls;

	@Mock
	private BlockService blockService;

	@InjectMocks
	private FeedService feedService;

	@Test
	void feedForUser_returnsEmpty_whenViewerProfileMissing() {
		when(profiles.findById(404L)).thenReturn(Optional.empty());
		assertTrue(feedService.feedForUser(404L).isEmpty());
	}

	@Test
	void feedForUser_returnsEmpty_whenNoCandidates() {
		long myId = 1L;
		ProfileEntity me = new ProfileEntity();
		me.setUserId(myId);
		me.setLatitude(35.68);
		me.setLongitude(139.76);
		me.setDistanceKm(50);
		me.setDiscoverySettings(Collections.emptyMap());
		when(profiles.findById(myId)).thenReturn(Optional.of(me));
		when(blockService.hiddenPeerIdsFor(myId)).thenReturn(Collections.emptySet());
		when(likeRepo.findByFromUser(myId)).thenReturn(List.of());
		when(passRepo.findByFromUser(myId)).thenReturn(List.of());
		when(photoRepo.countPhotosGroupByUserId()).thenReturn(List.of());
		when(profiles.findAll()).thenReturn(List.of(me));
		assertEquals(0, feedService.feedForUser(myId).size());
	}
}