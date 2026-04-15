package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.MessageEntity;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class MessageRepoTest {

	@Autowired
	private MessageRepo repo;

	@Test
	void countAndFindByMatchId() {
		MessageEntity a = new MessageEntity();
		a.setMatchId(50L);
		a.setSenderId(1L);
		a.setBody("hi");
		repo.save(a);

		MessageEntity b = new MessageEntity();
		b.setMatchId(50L);
		b.setSenderId(2L);
		b.setBody("yo");
		repo.save(b);

		assertEquals(2, repo.countByMatchId(50L));
		assertEquals(2, repo.findByMatchIdOrderByCreatedAtAsc(50L, PageRequest.of(0, 10)).size());
		assertTrue(repo.findFirstByMatchIdOrderByCreatedAtDesc(50L).isPresent());
	}

	@Test
	void countByMatchIdAndSenderIdNotAndCreatedAtAfter_excludesOwnRecent() {
		Instant base = Instant.now().minus(1, ChronoUnit.HOURS);
		MessageEntity olderPeer = new MessageEntity();
		olderPeer.setMatchId(60L);
		olderPeer.setSenderId(2L);
		olderPeer.setBody("old");
		olderPeer.setCreatedAt(base);
		repo.save(olderPeer);

		MessageEntity own = new MessageEntity();
		own.setMatchId(60L);
		own.setSenderId(1L);
		own.setBody("mine");
		own.setCreatedAt(base.plus(5, ChronoUnit.MINUTES));
		repo.save(own);

		long unread = repo.countByMatchIdAndSenderIdNotAndCreatedAtAfter(60L, 1L, base.plus(1, ChronoUnit.MINUTES));
		assertEquals(0L, unread);
	}
}
