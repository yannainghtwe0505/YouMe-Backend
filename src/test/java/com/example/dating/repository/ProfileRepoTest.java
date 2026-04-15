package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.ProfileEntity;

/**
 * Full application context: H2 DDL for {@link ProfileEntity} jsonb columns is exercised the same way as ITs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class ProfileRepoTest {

	@Autowired
	private ProfileRepo repo;

	@Test
	void saveAndFindById_roundTrip() {
		ProfileEntity p = new ProfileEntity();
		p.setUserId(700L);
		p.setDisplayName("Test User");
		repo.save(p);

		assertTrue(repo.findById(700L).isPresent());
		assertEquals("Test User", repo.findById(700L).orElseThrow().getDisplayName());
	}
}
