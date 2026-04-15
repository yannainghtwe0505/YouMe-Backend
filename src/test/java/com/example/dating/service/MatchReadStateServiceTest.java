package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.MatchReadState;
import com.example.dating.repository.MatchReadStateRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchReadStateServiceTest {

	@Mock
	private MatchReadStateRepo repo;

	@InjectMocks
	private MatchReadStateService matchReadStateService;

	@Test
	void seedNewMatch_savesTwoRowsForBothUsers() {
		matchReadStateService.seedNewMatch(500L, 10L, 20L);
		ArgumentCaptor<MatchReadState> cap = ArgumentCaptor.forClass(MatchReadState.class);
		verify(repo, times(2)).save(cap.capture());
		var saved = cap.getAllValues();
		assertEquals(10L, saved.get(0).getUserId());
		assertEquals(500L, saved.get(0).getMatchId());
		assertEquals(20L, saved.get(1).getUserId());
		assertEquals(500L, saved.get(1).getMatchId());
		assertNotNull(saved.get(0).getLastReadAt());
		assertEquals(saved.get(0).getLastReadAt(), saved.get(1).getLastReadAt());
	}

	@Test
	void markRead_createsRow_whenMissing() {
		when(repo.findByUserIdAndMatchId(1L, 2L)).thenReturn(java.util.Optional.empty());
		matchReadStateService.markRead(1L, 2L);
		ArgumentCaptor<MatchReadState> cap = ArgumentCaptor.forClass(MatchReadState.class);
		verify(repo).save(cap.capture());
		MatchReadState s = cap.getValue();
		assertEquals(1L, s.getUserId());
		assertEquals(2L, s.getMatchId());
		assertNotNull(s.getLastReadAt());
	}

	@Test
	void markRead_updatesTimestamp_whenExisting() {
		Instant stale = Instant.parse("2020-01-01T00:00:00Z");
		MatchReadState existing = new MatchReadState();
		existing.setUserId(3L);
		existing.setMatchId(4L);
		existing.setLastReadAt(stale);
		when(repo.findByUserIdAndMatchId(3L, 4L)).thenReturn(java.util.Optional.of(existing));
		matchReadStateService.markRead(3L, 4L);
		ArgumentCaptor<MatchReadState> cap = ArgumentCaptor.forClass(MatchReadState.class);
		verify(repo).save(cap.capture());
		assertNotEquals(stale, cap.getValue().getLastReadAt());
		assertNotNull(cap.getValue().getLastReadAt());
	}
}