package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.PendingRegistrationEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class PendingRegistrationRepoTest {

	@Autowired
	private PendingRegistrationRepo repo;

	@Test
	void findByEmail_andSessionToken() {
		PendingRegistrationEntity e = new PendingRegistrationEntity();
		e.setEmail("pend@example.com");
		e.setChannel("email");
		e.setCodeHash("hash");
		e.setExpiresAt(Instant.now().plusSeconds(3600));
		e.setSessionToken("tok-abc");
		repo.save(e);

		assertTrue(repo.findByEmail("pend@example.com").isPresent());
		assertEquals("pend@example.com", repo.findBySessionToken("tok-abc").orElseThrow().getEmail());
	}
}
