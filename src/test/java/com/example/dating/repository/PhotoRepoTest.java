package com.example.dating.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.PhotoEntity;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class PhotoRepoTest {

	@Autowired
	private PhotoRepo repo;

	@Test
	void findByUserIdOrderByCreatedAtAsc_andFindByIdAndUserId() {
		PhotoEntity p = new PhotoEntity();
		p.setUserId(80L);
		p.setS3Key("uploads/80/a.jpg");
		p.setPrimaryPhoto(true);
		repo.save(p);

		assertEquals(1, repo.findByUserIdOrderByCreatedAtAsc(80L).size());
		assertTrue(repo.findByIdAndUserId(p.getId(), 80L).isPresent());
		assertEquals(1L, repo.countByUserId(80L));
	}

	@Test
	void countPhotosGroupByUserId_aggregates() {
		PhotoEntity a = new PhotoEntity();
		a.setUserId(81L);
		a.setS3Key("k1");
		repo.save(a);
		PhotoEntity b = new PhotoEntity();
		b.setUserId(81L);
		b.setS3Key("k2");
		repo.save(b);

		List<Object[]> rows = repo.countPhotosGroupByUserId();
		assertEquals(1, rows.size());
		assertEquals(81L, rows.get(0)[0]);
		assertEquals(2L, ((Number) rows.get(0)[1]).longValue());
	}
}
