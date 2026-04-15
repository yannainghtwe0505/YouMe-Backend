package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.UserEntity;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class UserRepoTest {

	@Autowired
	private UserRepo repo;

	@Test
	void save_thenFindByEmail_returnsSameRow() {
		UserEntity u = new UserEntity();
		u.setEmail("repo-user@example.com");
		u.setPasswordHash("hash");
		repo.save(u);

		assertTrue(repo.findByEmail("repo-user@example.com").isPresent());
		assertEquals("repo-user@example.com", repo.findByEmail("repo-user@example.com").orElseThrow().getEmail());
	}

	@Test
	void save_withPhone_thenFindByPhoneE164() {
		UserEntity u = new UserEntity();
		u.setEmail("phone-user@example.com");
		u.setPasswordHash("hash");
		u.setPhoneE164("+819012345678");
		repo.save(u);

		assertTrue(repo.findByPhoneE164("+819012345678").isPresent());
	}
}
