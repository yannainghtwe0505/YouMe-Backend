package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserSubscriptionEntity;
import com.example.dating.model.subscription.BillingProvider;
import com.example.dating.model.subscription.SubscriptionLifecycleStatus;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.UserSubscriptionRepo;

import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionPlanServiceTest {

	@Mock
	private UserSubscriptionRepo userSubscriptionRepo;

	@InjectMocks
	private SubscriptionPlanService subscriptionPlanService;

	@Test
	void resolve_returnsFree_whenProfileNull() {
		assertEquals(SubscriptionPlan.FREE, subscriptionPlanService.resolve(null));
	}

	@Test
	void resolve_legacyPremiumMapsToPlus() {
		ProfileEntity p = new ProfileEntity();
		p.setUserId(null);
		p.setSubscriptionPlan("FREE");
		p.setPremium(true);
		assertEquals(SubscriptionPlan.PLUS, subscriptionPlanService.resolve(p));
	}

	@Test
	void resolve_legacyColumnPlus() {
		ProfileEntity p = new ProfileEntity();
		p.setUserId(null);
		p.setSubscriptionPlan("PLUS");
		p.setPremium(false);
		assertEquals(SubscriptionPlan.PLUS, subscriptionPlanService.resolve(p));
	}

	@Test
	void resolve_billingRowActivePlus() {
		long uid = 100L;
		ProfileEntity p = new ProfileEntity();
		p.setUserId(uid);
		UserSubscriptionEntity us = new UserSubscriptionEntity();
		us.setUserId(uid);
		us.setPlanTier("PLUS");
		us.setLifecycleStatus(SubscriptionLifecycleStatus.ACTIVE.name());
		us.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
		when(userSubscriptionRepo.findById(uid)).thenReturn(java.util.Optional.of(us));
		assertEquals(SubscriptionPlan.PLUS, subscriptionPlanService.resolve(p));
	}

	@Test
	void resolve_billingRowPendingReturnsFree() {
		long uid = 101L;
		ProfileEntity p = new ProfileEntity();
		p.setUserId(uid);
		p.setSubscriptionPlan("GOLD");
		UserSubscriptionEntity us = new UserSubscriptionEntity();
		us.setLifecycleStatus(SubscriptionLifecycleStatus.PENDING.name());
		us.setPlanTier("PLUS");
		when(userSubscriptionRepo.findById(uid)).thenReturn(java.util.Optional.of(us));
		assertEquals(SubscriptionPlan.FREE, subscriptionPlanService.resolve(p));
	}

	@Test
	void applyPlanToProfile_setsPremiumFlag() {
		ProfileEntity p = new ProfileEntity();
		subscriptionPlanService.applyPlanToProfile(p, SubscriptionPlan.GOLD);
		assertEquals("GOLD", p.getSubscriptionPlan());
		assertTrue(p.isPremium());
		subscriptionPlanService.applyPlanToProfile(p, SubscriptionPlan.FREE);
		assertEquals("FREE", p.getSubscriptionPlan());
		assertFalse(p.isPremium());
	}

	@Test
	void billingProviderForUser_noneWhenMissingRow() {
		when(userSubscriptionRepo.findById(55L)).thenReturn(java.util.Optional.empty());
		assertEquals(BillingProvider.NONE, subscriptionPlanService.billingProviderForUser(55L));
	}
}