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

import com.example.dating.model.entity.AiCoachDailyUsageEntity;
import com.example.dating.model.entity.AiCoachDailyUsagePk;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class AiCoachDailyUsageRepoTest {

	@Autowired
	private AiCoachDailyUsageRepo repo;

	@Test
	void saveAndFindByCompositeId() {
		AiCoachDailyUsagePk pk = new AiCoachDailyUsagePk(501L, LocalDate.of(2025, 3, 2));
		AiCoachDailyUsageEntity row = new AiCoachDailyUsageEntity();
		row.setId(pk);
		row.setUsageCount(2);
		repo.save(row);

		assertTrue(repo.findById(pk).isPresent());
		assertEquals(2, repo.findById(pk).orElseThrow().getUsageCount());
	}
}
