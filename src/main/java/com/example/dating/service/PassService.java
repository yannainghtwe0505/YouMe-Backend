package com.example.dating.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.PassEntity;
import com.example.dating.repository.PassRepo;

@Service
public class PassService {
	private final PassRepo passRepo;
	private final BlockService blockService;

	public PassService(PassRepo passRepo, BlockService blockService) {
		this.passRepo = passRepo;
		this.blockService = blockService;
	}

	@Transactional
	public void recordPass(Long fromUserId, Long toUserId) {
		if (fromUserId.equals(toUserId) || blockService.eitherBlocked(fromUserId, toUserId))
			return;
		if (passRepo.existsByFromUserAndToUser(fromUserId, toUserId))
			return;
		PassEntity p = new PassEntity();
		p.setFromUser(fromUserId);
		p.setToUser(toUserId);
		passRepo.save(p);
	}
}
