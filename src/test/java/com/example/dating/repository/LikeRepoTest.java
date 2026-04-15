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

import com.example.dating.model.entity.LikeEntity;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class LikeRepoTest {

	@Autowired
	private LikeRepo repo;

	@Test
	void existsByPair_andFindByFromUser() {
		LikeEntity like = new LikeEntity();
		like.setFromUser(30L);
		like.setToUser(31L);
		like.setSuperLike(false);
		repo.save(like);

		assertTrue(repo.existsByFromUserAndToUser(30L, 31L));
		assertFalse(repo.existsByFromUserAndToUser(30L, 32L));
		assertEquals(1, repo.findByFromUser(30L).size());
		assertEquals(1, repo.findByToUser(31L).size());
	}
}
