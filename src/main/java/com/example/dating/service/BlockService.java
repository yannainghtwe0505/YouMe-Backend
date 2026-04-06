package com.example.dating.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.BlockEntity;
import com.example.dating.model.entity.BlockId;
import com.example.dating.repository.BlockRepo;

@Service
public class BlockService {
	private final BlockRepo blockRepo;

	public BlockService(BlockRepo blockRepo) {
		this.blockRepo = blockRepo;
	}

	public boolean eitherBlocked(Long userIdA, Long userIdB) {
		if (userIdA == null || userIdB == null || userIdA.equals(userIdB))
			return true;
		return blockRepo.existsByIdBlockerIdAndIdBlockedId(userIdA, userIdB)
				|| blockRepo.existsByIdBlockerIdAndIdBlockedId(userIdB, userIdA);
	}

	public Set<Long> hiddenPeerIdsFor(Long me) {
		Set<Long> s = new HashSet<>();
		s.addAll(blockRepo.findBlockedIdsByBlocker(me));
		s.addAll(blockRepo.findBlockerIdsByBlocked(me));
		return s;
	}

	@Transactional
	public void block(Long blockerId, Long blockedId) {
		if (blockerId.equals(blockedId))
			return;
		BlockId id = new BlockId(blockerId, blockedId);
		if (blockRepo.existsById(id))
			return;
		BlockEntity b = new BlockEntity();
		b.setId(id);
		blockRepo.save(b);
	}

	@Transactional
	public boolean unblock(Long blockerId, Long blockedId) {
		BlockId id = new BlockId(blockerId, blockedId);
		if (!blockRepo.existsById(id))
			return false;
		blockRepo.deleteById(id);
		return true;
	}
}
