package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.PassEntity;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class PassRepoTest {

	@Autowired
	private PassRepo repo;

	@Test
	void existsByPair_andFindByFromUser() {
		PassEntity p = new PassEntity();
		p.setFromUser(11L);
		p.setToUser(12L);
		repo.save(p);

		assertTrue(repo.existsByFromUserAndToUser(11L, 12L));
		assertFalse(repo.existsByFromUserAndToUser(11L, 99L));
		assertEquals(1, repo.findByFromUser(11L).size());
	}
}
