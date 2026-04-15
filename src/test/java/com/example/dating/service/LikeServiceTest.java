package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.LikeEntity;
import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.LikeRepo;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.PassRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.LikeService.LikeOutcome;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LikeServiceTest {

	@Mock
	private LikeRepo likeRepo;

	@Mock
	private MatchRepo matchRepo;

	@Mock
	private ProfileRepo profileRepo;

	@Mock
	private PassRepo passRepo;

	@Mock
	private MatchReadStateService matchReadStateService;

	@Mock
	private BlockService blockService;

	@Mock
	private AssistantGreetingService assistantGreetingService;

	@Mock
	private ProfileAvatarService profileAvatarService;

	@Mock
	private SubscriptionPlanService subscriptionPlanService;

	@InjectMocks
	private LikeService likeService;

	@Test
	void likeAndMaybeMatch_returnsNoMatch_whenSelfLike() {
		LikeOutcome o = likeService.likeAndMaybeMatch(3L, 3L);
		assertFalse(o.matched());
		verify(likeRepo, never()).insertLikeIdempotent(anyLong(), anyLong(), anyBoolean());
	}

	@Test
	void likeAndMaybeMatch_returnsNoMatch_whenBlocked() {
		when(blockService.eitherBlocked(1L, 2L)).thenReturn(true);
		LikeOutcome o = likeService.likeAndMaybeMatch(1L, 2L);
		assertFalse(o.matched());
		verify(likeRepo, never()).insertLikeIdempotent(anyLong(), anyLong(), anyBoolean());
	}

	@Test
	void likeAndMaybeMatch_returnsMatched_whenReciprocalExists() {
		when(blockService.eitherBlocked(10L, 20L)).thenReturn(false);
		when(likeRepo.insertLikeIdempotent(10L, 20L, false)).thenReturn(1);
		when(likeRepo.existsByFromUserAndToUser(20L, 10L)).thenReturn(true);
		MatchEntity m = new MatchEntity();
		m.setId(500L);
		m.setUserA(10L);
		m.setUserB(20L);
		when(matchRepo.findByUserAAndUserB(10L, 20L)).thenReturn(java.util.Optional.of(m));
		LikeOutcome o = likeService.likeAndMaybeMatch(10L, 20L);
		assertTrue(o.matched());
		assertEquals(500L, o.matchId());
	}

	@Test
	void countInboundLikesForUser_excludesBlockedSenders() {
		long me = 7L;
		LikeEntity a = new LikeEntity();
		a.setFromUser(100L);
		a.setToUser(me);
		when(likeRepo.findByToUser(me)).thenReturn(List.of(a));
		when(blockService.eitherBlocked(me, 100L)).thenReturn(true);
		assertEquals(0L, likeService.countInboundLikesForUser(me));
	}

	@Test
	void getInboundLikesPayloadForViewer_locksForFreePlan() {
		when(profileRepo.findById(9L)).thenReturn(Optional.of(new ProfileEntity()));
		when(subscriptionPlanService.resolve(any(ProfileEntity.class)))
				.thenReturn(com.example.dating.model.subscription.SubscriptionPlan.FREE);
		when(likeRepo.findByToUser(9L)).thenReturn(List.of());
		var payload = likeService.getInboundLikesPayloadForViewer(9L);
		assertTrue((Boolean) payload.get("locked"));
		assertEquals(0L, payload.get("likes_count"));
	}
}