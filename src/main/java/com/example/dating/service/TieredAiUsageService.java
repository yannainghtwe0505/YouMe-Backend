package com.example.dating.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.config.AiProperties;
import com.example.dating.dto.AiQuotaView;
import com.example.dating.error.AiCoachQuotaExceededException;
import com.example.dating.model.entity.AiFeatureDailyUsageEntity;
import com.example.dating.model.entity.AiFeatureDailyUsagePk;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.AiFeatureDailyUsageRepo;

@Service
public class TieredAiUsageService {
	private final AiFeatureDailyUsageRepo repo;
	private final AiProperties props;
	private final StringRedisTemplate redisTemplate;

	public TieredAiUsageService(AiFeatureDailyUsageRepo repo, AiProperties props,
			@Autowired(required = false) StringRedisTemplate redisTemplate) {
		this.repo = repo;
		this.props = props;
		this.redisTemplate = redisTemplate;
	}

	private boolean useRedis() {
		return props.isRedisUsageEnabled() && redisTemplate != null;
	}

	private ZoneId zone() {
		String tz = props.getUsageTimezone();
		try {
			return ZoneId.of(tz != null && !tz.isBlank() ? tz : "UTC");
		} catch (Exception e) {
			return ZoneId.of("UTC");
		}
	}

	private LocalDate today() {
		return LocalDate.now(zone());
	}

	private String redisKey(long userId, AiFeature feature, LocalDate day) {
		return "youme:ai:" + userId + ":" + feature.name() + ":" + day;
	}

	private long ttlSecondsToEndOfUsageDay(LocalDate usageDay) {
		ZoneId z = zone();
		ZonedDateTime end = usageDay.plusDays(1).atStartOfDay(z);
		long sec = Duration.between(ZonedDateTime.now(z), end).getSeconds();
		return Math.max(120L, sec);
	}

	public AiQuotaView status(long userId, SubscriptionPlan plan, AiFeature feature) {
		int used = readUsed(userId, feature, today());
		boolean fair = props.isUnlimitedTierLimit(plan, feature);
		int cap = props.effectiveDailyLimit(plan, feature);
		int remaining = Math.max(0, cap - used);
		if (fair)
			return AiQuotaView.fairUse(used, cap, remaining);
		return AiQuotaView.limited(used, cap, remaining);
	}

	public void assertCanUse(long userId, SubscriptionPlan plan, AiFeature feature) {
		AiQuotaView q = status(userId, plan, feature);
		if (q.remaining() <= 0) {
			throw new AiCoachQuotaExceededException(q, feature, plan, plan.suggestedUpgrade());
		}
	}

	@Transactional
	public void recordUse(long userId, AiFeature feature) {
		LocalDate d = today();
		if (useRedis()) {
			String key = redisKey(userId, feature, d);
			Long n = redisTemplate.opsForValue().increment(key);
			if (n != null && n == 1L) {
				redisTemplate.expire(key, ttlSecondsToEndOfUsageDay(d), TimeUnit.SECONDS);
			}
			return;
		}
		AiFeatureDailyUsagePk pk = new AiFeatureDailyUsagePk(userId, d, feature.name());
		AiFeatureDailyUsageEntity row = repo.findById(pk).orElseGet(() -> {
			AiFeatureDailyUsageEntity e = new AiFeatureDailyUsageEntity();
			e.setId(pk);
			e.setUsageCount(0);
			return e;
		});
		row.setUsageCount(row.getUsageCount() + 1);
		repo.save(row);
	}

	private int readUsed(long userId, AiFeature feature, LocalDate d) {
		if (useRedis()) {
			String key = redisKey(userId, feature, d);
			String v = redisTemplate.opsForValue().get(key);
			if (v != null) {
				try {
					return Integer.parseInt(v.trim());
				} catch (NumberFormatException ignored) {
					return 0;
				}
			}
			return jdbcUsed(userId, feature, d);
		}
		return jdbcUsed(userId, feature, d);
	}

	private int jdbcUsed(long userId, AiFeature feature, LocalDate d) {
		return repo.findById(new AiFeatureDailyUsagePk(userId, d, feature.name()))
				.map(AiFeatureDailyUsageEntity::getUsageCount)
				.orElse(0);
	}

	/** All metered features for /me and settings screens. */
	public Map<String, Object> entitlementsMap(long userId, SubscriptionPlan plan) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (AiFeature f : AiFeature.values()) {
			AiQuotaView q = status(userId, plan, f);
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("usedToday", q.usedToday());
			row.put("dailyLimit", q.dailyLimit());
			row.put("remaining", q.remaining());
			row.put("fairUseCap", q.fairUseCap());
			out.put(f.apiKey(), row);
		}
		return out;
	}
}
