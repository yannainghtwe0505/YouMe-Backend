package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.BlockEntity;
import com.example.dating.model.entity.BlockId;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class BlockRepoTest {

	@Autowired
	private BlockRepo repo;

	@Test
	void existsAndQueryMethods_reflectPersistedBlock() {
		BlockEntity b = new BlockEntity();
		b.setId(new BlockId(10L, 20L));
		repo.save(b);

		assertTrue(repo.existsByIdBlockerIdAndIdBlockedId(10L, 20L));
		assertEquals(List.of(20L), repo.findBlockedIdsByBlocker(10L));
		assertEquals(List.of(10L), repo.findBlockerIdsByBlocked(20L));
	}

	@Test
	void existsByPair_returnsFalseWhenAbsent() {
		assertFalse(repo.existsByIdBlockerIdAndIdBlockedId(1L, 2L));
	}
}
