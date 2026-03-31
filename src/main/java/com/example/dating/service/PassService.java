package com.example.dating.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.PassEntity;
import com.example.dating.repository.PassRepo;

@Service
public class PassService {
	private final PassRepo passRepo;

	public PassService(PassRepo passRepo) {
		this.passRepo = passRepo;
	}

	@Transactional
	public void recordPass(Long fromUserId, Long toUserId) {
		if (fromUserId.equals(toUserId))
			return;
		if (passRepo.existsByFromUserAndToUser(fromUserId, toUserId))
			return;
		PassEntity p = new PassEntity();
		p.setFromUser(fromUserId);
		p.setToUser(toUserId);
		passRepo.save(p);
	}
}
