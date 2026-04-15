package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.UserSubscriptionEntity;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class UserSubscriptionRepoTest {

	@Autowired
	private UserSubscriptionRepo repo;

	@Test
	void findByExternalSubscriptionId() {
		UserSubscriptionEntity e = new UserSubscriptionEntity();
		e.setUserId(200L);
		e.setPlanTier("PLUS");
		e.setExternalSubscriptionId("sub_ext_1");
		e.setLifecycleStatus("ACTIVE");
		repo.save(e);

		assertTrue(repo.findByExternalSubscriptionId("sub_ext_1").isPresent());
		assertEquals("PLUS", repo.findByExternalSubscriptionId("sub_ext_1").orElseThrow().getPlanTier());
	}
}
