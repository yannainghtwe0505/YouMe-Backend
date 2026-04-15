package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.AiFeatureDailyUsageEntity;
import com.example.dating.model.entity.AiFeatureDailyUsagePk;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class AiFeatureDailyUsageRepoTest {

	@Autowired
	private AiFeatureDailyUsageRepo repo;

	@Test
	void saveAndFindByCompositeId() {
		AiFeatureDailyUsagePk pk = new AiFeatureDailyUsagePk(500L, LocalDate.of(2025, 3, 1), "CHAT_REPLY");
		AiFeatureDailyUsageEntity row = new AiFeatureDailyUsageEntity();
		row.setId(pk);
		row.setUsageCount(3);
		repo.save(row);

		assertTrue(repo.findById(pk).isPresent());
		assertEquals(3, repo.findById(pk).orElseThrow().getUsageCount());
	}
}
