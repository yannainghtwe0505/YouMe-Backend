package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.PassEntity;
import com.example.dating.repository.PassRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassServiceTest {

	@Mock
	private PassRepo passRepo;

	@Mock
	private BlockService blockService;

	@InjectMocks
	private PassService passService;

	@Test
	void recordPass_skips_whenSelfPass() {
		passService.recordPass(5L, 5L);
		verify(passRepo, never()).save(any());
		verify(passRepo, never()).existsByFromUserAndToUser(any(), any());
	}

	@Test
	void recordPass_skips_whenEitherBlocked() {
		when(blockService.eitherBlocked(1L, 2L)).thenReturn(true);
		passService.recordPass(1L, 2L);
		verify(passRepo, never()).save(any());
		verify(passRepo, never()).existsByFromUserAndToUser(any(), any());
	}

	@Test
	void recordPass_skips_whenDuplicate() {
		when(blockService.eitherBlocked(8L, 9L)).thenReturn(false);
		when(passRepo.existsByFromUserAndToUser(8L, 9L)).thenReturn(true);
		passService.recordPass(8L, 9L);
		verify(passRepo, never()).save(any());
	}

	@Test
	void recordPass_saves_whenNewAndNotBlocked() {
		when(blockService.eitherBlocked(100L, 200L)).thenReturn(false);
		when(passRepo.existsByFromUserAndToUser(100L, 200L)).thenReturn(false);
		assertDoesNotThrow(() -> passService.recordPass(100L, 200L));
		ArgumentCaptor<PassEntity> cap = ArgumentCaptor.forClass(PassEntity.class);
		verify(passRepo).save(cap.capture());
		PassEntity saved = cap.getValue();
		assertEquals(100L, saved.getFromUser());
		assertEquals(200L, saved.getToUser());
	}
}