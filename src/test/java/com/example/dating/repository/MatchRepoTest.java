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

import com.example.dating.model.entity.MatchEntity;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class MatchRepoTest {

	@Autowired
	private MatchRepo repo;

	@Test
	void findByUserAAndUserB_roundTrip() {
		MatchEntity m = new MatchEntity();
		m.setUserA(3L);
		m.setUserB(4L);
		repo.save(m);

		assertTrue(repo.findByUserAAndUserB(3L, 4L).isPresent());
		assertTrue(repo.existsByUserAAndUserB(3L, 4L));
	}

	@Test
	void findAllByUserInvolved_returnsRowsForEitherSide() {
		MatchEntity m = new MatchEntity();
		m.setUserA(5L);
		m.setUserB(6L);
		repo.save(m);

		assertEquals(1, repo.findAllByUserInvolved(5L).size());
		assertEquals(1, repo.findAllByUserInvolved(6L).size());
		assertTrue(repo.findAllByUserInvolved(99L).isEmpty());
	}
}
