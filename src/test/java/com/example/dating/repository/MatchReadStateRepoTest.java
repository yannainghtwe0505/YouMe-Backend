package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.MatchReadState;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class MatchReadStateRepoTest {

	@Autowired
	private MatchReadStateRepo repo;

	@Test
	void findByUserIdAndMatchId_returnsRow() {
		Instant t = Instant.parse("2025-06-01T12:00:00Z");
		MatchReadState s = new MatchReadState();
		s.setUserId(9L);
		s.setMatchId(100L);
		s.setLastReadAt(t);
		repo.save(s);

		assertTrue(repo.findByUserIdAndMatchId(9L, 100L).isPresent());
		assertEquals(t, repo.findByUserIdAndMatchId(9L, 100L).orElseThrow().getLastReadAt());
	}
}
