package com.example.dating.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.model.entity.UserHelpEventEntity;
import com.example.dating.repository.UserHelpEventRepo;

@Service
public class HelpAnalyticsService {
	private static final Logger log = LoggerFactory.getLogger(HelpAnalyticsService.class);
	private static final int MAX_BATCH = 25;
	private static final int MAX_NAME_LEN = 80;

	private final UserHelpEventRepo events;

	public HelpAnalyticsService(UserHelpEventRepo events) {
		this.events = events;
	}

	@Transactional
	public int recordBatch(long userId, List<Map<String, Object>> batch) {
		if (batch == null || batch.isEmpty()) {
			return 0;
		}
		int n = 0;
		for (Map<String, Object> item : batch) {
			if (n >= MAX_BATCH) {
				break;
			}
			String name = item == null ? null : String.valueOf(item.getOrDefault("name", ""));
			if (name.isBlank() || "null".equals(name)) {
				continue;
			}
			name = name.trim();
			if (name.length() > MAX_NAME_LEN) {
				name = name.substring(0, MAX_NAME_LEN);
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> props = item.get("properties") instanceof Map<?, ?> m
					? new HashMap<>((Map<String, Object>) m)
					: Map.of();
			UserHelpEventEntity row = new UserHelpEventEntity();
			row.setUserId(userId);
			row.setEventName(name);
			row.setProperties(props);
			events.save(row);
			log.debug("help.event userId={} name={}", userId, name);
			n++;
		}
		return n;
	}
}
