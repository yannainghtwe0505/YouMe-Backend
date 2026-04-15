package com.example.dating.dev;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class MatchReadStateDevBackfillTest {

	@Test
	void runSwallowsMissingTable() {
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.queryForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("no table"));
		MatchReadStateDevBackfill backfill = new MatchReadStateDevBackfill(jdbc);
		assertDoesNotThrow(() -> backfill.run(mock(ApplicationArguments.class)));
	}
}
