package com.example.dating.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * When the {@code dev} profile is active, ensures {@code match_read_state} has rows for existing matches
 * (equivalent to V2 backfill) if the table was just created by Hibernate.
 */
@Component
@Profile("dev")
public class MatchReadStateDevBackfill implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(MatchReadStateDevBackfill.class);
	private final JdbcTemplate jdbc;

	public MatchReadStateDevBackfill(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			Long m = jdbc.queryForObject("select count(*) from match_read_state", Long.class);
			if (m != null && m > 0) {
				return;
			}
			int a = jdbc.update("""
					insert into match_read_state (user_id, match_id, last_read_at)
					select user_a, id, now() from matches
					on conflict do nothing
					""");
			int b = jdbc.update("""
					insert into match_read_state (user_id, match_id, last_read_at)
					select user_b, id, now() from matches
					on conflict do nothing
					""");
			log.info("Dev backfill: seeded match_read_state ({} + {} rows)", a, b);
		} catch (Exception e) {
			log.warn("Dev match_read_state backfill skipped: {}", e.getMessage());
		}
	}
}
