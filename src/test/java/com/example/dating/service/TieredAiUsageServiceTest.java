package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.config.AiProperties;
import com.example.dating.dto.AiQuotaView;
import com.example.dating.error.AiCoachQuotaExceededException;
import com.example.dating.model.entity.AiFeatureDailyUsageEntity;
import com.example.dating.model.entity.AiFeatureDailyUsagePk;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.AiFeatureDailyUsageRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TieredAiUsageServiceTest {

	@Mock
	private AiFeatureDailyUsageRepo repo;

	@Mock
	private AiProperties props;

	@InjectMocks
	private TieredAiUsageService tieredAiUsageService;

	private void stubJdbcDefaults() {
		when(props.isRedisUsageEnabled()).thenReturn(false);
		when(props.getUsageTimezone()).thenReturn("UTC");
	}

	@Test
	void assertCanUse_throws_whenNoRemainingQuota() {
		stubJdbcDefaults();
		when(props.effectiveDailyLimit(SubscriptionPlan.FREE, AiFeature.CHAT_REPLY)).thenReturn(2);
		when(props.isUnlimitedTierLimit(SubscriptionPlan.FREE, AiFeature.CHAT_REPLY)).thenReturn(false);
		AiFeatureDailyUsageEntity row = new AiFeatureDailyUsageEntity();
		row.setUsageCount(2);
		when(repo.findById(any(AiFeatureDailyUsagePk.class))).thenReturn(Optional.of(row));
		assertThrows(AiCoachQuotaExceededException.class,
				() -> tieredAiUsageService.assertCanUse(1L, SubscriptionPlan.FREE, AiFeature.CHAT_REPLY));
	}

	@Test
	void status_reportsRemainingAgainstCap() {
		stubJdbcDefaults();
		when(props.effectiveDailyLimit(SubscriptionPlan.PLUS, AiFeature.CHAT_REPLY)).thenReturn(10);
		when(props.isUnlimitedTierLimit(SubscriptionPlan.PLUS, AiFeature.CHAT_REPLY)).thenReturn(false);
		AiFeatureDailyUsageEntity row = new AiFeatureDailyUsageEntity();
		row.setUsageCount(3);
		when(repo.findById(any(AiFeatureDailyUsagePk.class))).thenReturn(Optional.of(row));
		AiQuotaView q = tieredAiUsageService.status(88L, SubscriptionPlan.PLUS, AiFeature.CHAT_REPLY);
		assertEquals(3, q.usedToday());
		assertEquals(10, q.dailyLimit());
		assertEquals(7, q.remaining());
	}

	@Test
	void recordUse_persistsIncrementedJdbcRow() {
		stubJdbcDefaults();
		when(repo.findById(any(AiFeatureDailyUsagePk.class))).thenReturn(Optional.empty());
		tieredAiUsageService.recordUse(5L, AiFeature.PROFILE_AI);
		ArgumentCaptor<AiFeatureDailyUsageEntity> cap = ArgumentCaptor.forClass(AiFeatureDailyUsageEntity.class);
		verify(repo).save(cap.capture());
		assertEquals(1, cap.getValue().getUsageCount());
	}

	@Test
	void entitlementsMap_containsAllFeatures() {
		stubJdbcDefaults();
		when(props.effectiveDailyLimit(any(), any())).thenReturn(5);
		when(props.isUnlimitedTierLimit(any(), any())).thenReturn(false);
		when(repo.findById(any(AiFeatureDailyUsagePk.class))).thenReturn(Optional.empty());
		assertEquals(AiFeature.values().length,
				tieredAiUsageService.entitlementsMap(1L, SubscriptionPlan.FREE).size());
	}
}