package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserSubscriptionEntity;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserSubscriptionRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserSubscriptionServiceTest {

	@Mock
	private UserSubscriptionRepo userSubscriptionRepo;

	@Mock
	private ProfileRepo profileRepo;

	@Mock
	private SubscriptionPlanService subscriptionPlanService;

	@InjectMocks
	private UserSubscriptionService userSubscriptionService;

	@Test
	void ensureRow_createsFreeRow_whenMissing() {
		when(userSubscriptionRepo.findById(9L)).thenReturn(Optional.empty());
		when(userSubscriptionRepo.save(any(UserSubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
		UserSubscriptionEntity row = userSubscriptionService.ensureRow(9L);
		assertEquals(9L, row.getUserId());
		assertEquals("FREE", row.getPlanTier());
		verify(userSubscriptionRepo).save(any(UserSubscriptionEntity.class));
	}

	@Test
	void ensureRow_returnsExisting_whenPresent() {
		UserSubscriptionEntity existing = new UserSubscriptionEntity();
		existing.setUserId(3L);
		existing.setPlanTier("PLUS");
		when(userSubscriptionRepo.findById(3L)).thenReturn(Optional.of(existing));
		assertEquals("PLUS", userSubscriptionService.ensureRow(3L).getPlanTier());
		verify(userSubscriptionRepo, never()).save(any());
	}

	@Test
	void syncProfileFromBilling_noOp_whenProfileMissing() {
		when(profileRepo.findById(1L)).thenReturn(Optional.empty());
		userSubscriptionService.syncProfileFromBilling(1L);
		verify(subscriptionPlanService, never()).resolve(any());
		verify(profileRepo, never()).save(any());
	}

	@Test
	void syncProfileFromBilling_updatesProfile() {
		ProfileEntity p = new ProfileEntity();
		p.setUserId(2L);
		when(profileRepo.findById(2L)).thenReturn(Optional.of(p));
		when(subscriptionPlanService.resolve(p)).thenReturn(SubscriptionPlan.PLUS);
		userSubscriptionService.syncProfileFromBilling(2L);
		verify(subscriptionPlanService).applyPlanToProfile(p, SubscriptionPlan.PLUS);
		verify(profileRepo).save(p);
	}
}