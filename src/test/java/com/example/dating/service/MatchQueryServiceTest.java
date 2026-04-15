package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.MatchEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.MatchReadStateRepo;
import com.example.dating.repository.MatchRepo;
import com.example.dating.repository.MessageRepo;
import com.example.dating.repository.ProfileRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchQueryServiceTest {

	@Mock
	private MatchRepo matchRepo;
	@Mock
	private ProfileRepo profileRepo;
	@Mock
	private MessageRepo messageRepo;
	@Mock
	private MatchReadStateRepo readStateRepo;
	@Mock
	private BlockService blockService;
	@Mock
	private ProfileAvatarService profileAvatarService;

	@InjectMocks
	private MatchQueryService matchQueryService;

	private static MatchEntity match(long id, long userA, long userB) {
		MatchEntity m = new MatchEntity();
		m.setId(id);
		m.setUserA(userA);
		m.setUserB(userB);
		return m;
	}

	@Test
	void listMatchesForUser_hidesBlockedPeers() {
		long me = 1L;
		when(blockService.hiddenPeerIdsFor(me)).thenReturn(Set.of(99L));
		when(matchRepo.findAllByUserInvolved(me)).thenReturn(List.of(match(10L, me, 99L)));
		assertTrue(matchQueryService.listMatchesForUser(me).isEmpty());
	}

	@Test
	void listMatchesForUser_includesPeerSummary() {
		long me = 5L;
		MatchEntity m = match(20L, me, 7L);
		when(blockService.hiddenPeerIdsFor(me)).thenReturn(Set.of());
		when(matchRepo.findAllByUserInvolved(me)).thenReturn(List.of(m));
		ProfileEntity peer = new ProfileEntity();
		peer.setUserId(7L);
		peer.setDisplayName("Alex Tanaka");
		when(profileRepo.findById(7L)).thenReturn(Optional.of(peer));
		when(readStateRepo.findByUserIdAndMatchId(me, 20L)).thenReturn(Optional.empty());
		when(messageRepo.countByMatchIdAndSenderIdNotAndCreatedAtAfter(eq(20L), eq(me), eq(Instant.EPOCH)))
				.thenReturn(0L);
		when(messageRepo.findFirstByMatchIdOrderByCreatedAtDesc(20L)).thenReturn(Optional.empty());
		when(profileAvatarService.resolveAvatarUrl(eq(7L), eq(peer))).thenReturn("https://cdn.example/avatar.jpg");

		List<Map<String, Object>> rows = matchQueryService.listMatchesForUser(me);
		assertEquals(1, rows.size());
		assertEquals(7L, rows.get(0).get("peerUserId"));
		assertEquals("Alex Tanaka", rows.get(0).get("peerName"));
		assertEquals("https://cdn.example/avatar.jpg", rows.get(0).get("peerAvatar"));
		assertEquals(0L, rows.get(0).get("unreadCount"));
	}

	@Test
	void totalUnreadForUser_skipsHiddenPeers() {
		long me = 2L;
		when(blockService.hiddenPeerIdsFor(me)).thenReturn(Set.of(8L));
		when(matchRepo.findAllByUserInvolved(me))
				.thenReturn(List.of(match(30L, me, 8L), match(31L, me, 9L)));
		when(readStateRepo.findByUserIdAndMatchId(eq(me), any())).thenReturn(Optional.empty());
		when(messageRepo.countByMatchIdAndSenderIdNotAndCreatedAtAfter(eq(31L), eq(me), eq(Instant.EPOCH)))
				.thenReturn(3L);
		assertEquals(3L, matchQueryService.totalUnreadForUser(me));
		verify(messageRepo, never()).countByMatchIdAndSenderIdNotAndCreatedAtAfter(eq(30L), any(), any());
	}

	@Test
	void peerUserIdForMatch_emptyWhenStranger() {
		when(matchRepo.findById(1L)).thenReturn(Optional.of(match(1L, 10L, 11L)));
		assertTrue(matchQueryService.peerUserIdForMatch(1L, 99L).isEmpty());
	}

	@Test
	void peerUserIdForMatch_returnsPeerId() {
		when(matchRepo.findById(2L)).thenReturn(Optional.of(match(2L, 10L, 11L)));
		assertEquals(11L, matchQueryService.peerUserIdForMatch(2L, 10L).orElseThrow());
	}

	@Test
	void userParticipatesInMatch() {
		when(matchRepo.findById(3L)).thenReturn(Optional.of(match(3L, 50L, 60L)));
		assertTrue(matchQueryService.userParticipatesInMatch(50L, 3L));
		assertFalse(matchQueryService.userParticipatesInMatch(99L, 3L));
	}

	@Test
	void deleteMatchIfParticipant_deletesWhenMember() {
		MatchEntity m = match(4L, 1L, 2L);
		when(matchRepo.findById(4L)).thenReturn(Optional.of(m));
		assertTrue(matchQueryService.deleteMatchIfParticipant(1L, 4L));
		verify(matchRepo).delete(m);
	}

	@Test
	void deleteMatchIfParticipant_returnsFalseWhenNotParticipant() {
		when(matchRepo.findById(5L)).thenReturn(Optional.of(match(5L, 1L, 2L)));
		assertFalse(matchQueryService.deleteMatchIfParticipant(99L, 5L));
		verify(matchRepo, never()).delete(any());
	}
}