package com.example.dating.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
	/**
	 * When false, only local templates are used.
	 */
	private boolean enabled = true;
	/** OpenAI-compatible API key (also read OPENAI_API_KEY env in yaml). */
	private String apiKey = "";
	private String baseUrl = "https://api.openai.com/v1";
	private String model = "gpt-4o-mini";
	/** IANA zone id for daily reset (e.g. Asia/Tokyo). */
	private String usageTimezone = "Asia/Tokyo";

	/** When true, usage counters use Redis (see {@link #redis}); JDBC used as fallback if Redis fails. */
	private boolean redisUsageEnabled = false;
	private final RedisProps redis = new RedisProps();

	/** Soft cap per day when tier limit is {@code -1} (unlimited). */
	private int goldFairUseDailyCap = 500;

	private TierQuotas freeTier = new TierQuotas(3, 1, 1, 0);
	private TierQuotas plusTier = new TierQuotas(20, 5, 10, 20);
	private TierQuotas goldTier = new TierQuotas(-1, -1, -1, -1);

	/** @deprecated use tier quotas; kept for YAML compatibility */
	@SuppressWarnings("unused")
	private int freeDailyCoachUses = 3;
	/** @deprecated use tier quotas */
	@SuppressWarnings("unused")
	private int premiumDailyCoachUses = 20;

	public static final class RedisProps {
		private String host = "localhost";
		private int port = 6379;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}
	}

	/** Per-plan daily limits; {@code -1} means unlimited subject to {@link #goldFairUseDailyCap}. */
	public static final class TierQuotas {
		private int chatReplies;
		private int profileAi;
		private int matchGreetings;
		private int matchInsights;

		public TierQuotas() {
		}

		public TierQuotas(int chatReplies, int profileAi, int matchGreetings, int matchInsights) {
			this.chatReplies = chatReplies;
			this.profileAi = profileAi;
			this.matchGreetings = matchGreetings;
			this.matchInsights = matchInsights;
		}

		public int getChatReplies() {
			return chatReplies;
		}

		public void setChatReplies(int chatReplies) {
			this.chatReplies = chatReplies;
		}

		public int getProfileAi() {
			return profileAi;
		}

		public void setProfileAi(int profileAi) {
			this.profileAi = profileAi;
		}

		public int getMatchGreetings() {
			return matchGreetings;
		}

		public void setMatchGreetings(int matchGreetings) {
			this.matchGreetings = matchGreetings;
		}

		public int getMatchInsights() {
			return matchInsights;
		}

		public void setMatchInsights(int matchInsights) {
			this.matchInsights = matchInsights;
		}

		public int limit(AiFeature feature) {
			return switch (feature) {
				case CHAT_REPLY -> chatReplies;
				case PROFILE_AI -> profileAi;
				case MATCH_GREETING -> matchGreetings;
				case MATCH_INSIGHT -> matchInsights;
			};
		}
	}

	public TierQuotas quotasFor(SubscriptionPlan plan) {
		return switch (plan) {
			case FREE -> freeTier;
			case PLUS -> plusTier;
			case GOLD -> goldTier;
		};
	}

	/**
	 * Effective numeric cap for the day (never negative except we map -1 to fair-use cap).
	 */
	public int effectiveDailyLimit(SubscriptionPlan plan, AiFeature feature) {
		int raw = quotasFor(plan).limit(feature);
		if (raw < 0)
			return goldFairUseDailyCap;
		return raw;
	}

	public boolean isUnlimitedTierLimit(SubscriptionPlan plan, AiFeature feature) {
		return quotasFor(plan).limit(feature) < 0;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

	public String getUsageTimezone() {
		return usageTimezone;
	}

	public void setUsageTimezone(String usageTimezone) {
		this.usageTimezone = usageTimezone;
	}

	public boolean isRedisUsageEnabled() {
		return redisUsageEnabled;
	}

	public void setRedisUsageEnabled(boolean redisUsageEnabled) {
		this.redisUsageEnabled = redisUsageEnabled;
	}

	public RedisProps getRedis() {
		return redis;
	}

	public int getGoldFairUseDailyCap() {
		return goldFairUseDailyCap;
	}

	public void setGoldFairUseDailyCap(int goldFairUseDailyCap) {
		this.goldFairUseDailyCap = goldFairUseDailyCap;
	}

	public TierQuotas getFreeTier() {
		return freeTier;
	}

	public TierQuotas getPlusTier() {
		return plusTier;
	}

	public TierQuotas getGoldTier() {
		return goldTier;
	}

	public int getFreeDailyCoachUses() {
		return freeDailyCoachUses;
	}

	public void setFreeDailyCoachUses(int freeDailyCoachUses) {
		this.freeDailyCoachUses = freeDailyCoachUses;
	}

	public int getPremiumDailyCoachUses() {
		return premiumDailyCoachUses;
	}

	public void setPremiumDailyCoachUses(int premiumDailyCoachUses) {
		this.premiumDailyCoachUses = premiumDailyCoachUses;
	}
}
