package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.BlockEntity;
import com.example.dating.model.entity.BlockId;
import com.example.dating.repository.BlockRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlockServiceTest {

	@Mock
	private BlockRepo blockRepo;

	@InjectMocks
	private BlockService blockService;

	@Test
	void eitherBlocked_returnsTrue_whenEitherDirectionExists() {
		when(blockRepo.existsByIdBlockerIdAndIdBlockedId(10L, 20L)).thenReturn(true);
		when(blockRepo.existsByIdBlockerIdAndIdBlockedId(20L, 10L)).thenReturn(false);
		assertTrue(blockService.eitherBlocked(10L, 20L));
	}

	@Test
	void eitherBlocked_returnsTrue_whenReverseDirectionExists() {
		when(blockRepo.existsByIdBlockerIdAndIdBlockedId(10L, 20L)).thenReturn(false);
		when(blockRepo.existsByIdBlockerIdAndIdBlockedId(20L, 10L)).thenReturn(true);
		assertTrue(blockService.eitherBlocked(10L, 20L));
	}

	@Test
	void eitherBlocked_returnsFalse_whenNoBlockBetweenPeers() {
		when(blockRepo.existsByIdBlockerIdAndIdBlockedId(5L, 6L)).thenReturn(false);
		when(blockRepo.existsByIdBlockerIdAndIdBlockedId(6L, 5L)).thenReturn(false);
		assertFalse(blockService.eitherBlocked(5L, 6L));
	}

	@Test
	void eitherBlocked_returnsTrue_whenSameUser() {
		assertTrue(blockService.eitherBlocked(7L, 7L));
		verify(blockRepo, never()).existsByIdBlockerIdAndIdBlockedId(any(), any());
	}

	@Test
	void eitherBlocked_returnsTrue_whenNullInput() {
		assertTrue(blockService.eitherBlocked(null, 1L));
		assertTrue(blockService.eitherBlocked(1L, null));
		verify(blockRepo, never()).existsByIdBlockerIdAndIdBlockedId(any(), any());
	}

	@Test
	void hiddenPeerIdsFor_unionsBlockedAndBlockerLists() {
		when(blockRepo.findBlockedIdsByBlocker(100L)).thenReturn(List.of(2L, 3L));
		when(blockRepo.findBlockerIdsByBlocked(100L)).thenReturn(List.of(4L));
		Set<Long> hidden = blockService.hiddenPeerIdsFor(100L);
		assertEquals(Set.of(2L, 3L, 4L), hidden);
		verify(blockRepo).findBlockedIdsByBlocker(100L);
		verify(blockRepo).findBlockerIdsByBlocked(100L);
	}

	@Test
	void block_skips_whenSelfBlock() {
		blockService.block(9L, 9L);
		verify(blockRepo, never()).save(any());
		verify(blockRepo, never()).existsById(any());
	}

	@Test
	void block_skips_whenAlreadyExists() {
		BlockId id = new BlockId(1L, 2L);
		when(blockRepo.existsById(id)).thenReturn(true);
		blockService.block(1L, 2L);
		verify(blockRepo, never()).save(any());
	}

	@Test
	void block_persists_whenNew() {
		BlockId id = new BlockId(11L, 22L);
		when(blockRepo.existsById(id)).thenReturn(false);
		blockService.block(11L, 22L);
		ArgumentCaptor<BlockEntity> cap = ArgumentCaptor.forClass(BlockEntity.class);
		verify(blockRepo).save(cap.capture());
		assertEquals(id, cap.getValue().getId());
	}

	@Test
	void unblock_returnsFalse_whenMissing() {
		when(blockRepo.existsById(new BlockId(3L, 4L))).thenReturn(false);
		assertFalse(blockService.unblock(3L, 4L));
		verify(blockRepo, never()).deleteById(any());
	}

	@Test
	void unblock_returnsTrue_andDeletes_whenPresent() {
		BlockId id = new BlockId(3L, 4L);
		when(blockRepo.existsById(id)).thenReturn(true);
		assertTrue(blockService.unblock(3L, 4L));
		verify(blockRepo).deleteById(id);
	}
}