package com.example.dating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.config.AiProperties;
import com.example.dating.config.AppUrlsProperties;
import com.example.dating.config.SubscriptionProperties;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

	@Mock
	private SubscriptionProperties subscriptionProperties;

	@Mock
	private AppUrlsProperties appUrlsProperties;

	@Mock
	private AiProperties aiProperties;

	@Mock
	private ProfileRepo profileRepo;

	@Mock
	private UserRepo userRepo;

	@Mock
	private SubscriptionPlanService subscriptionPlanService;

	@Mock
	private UserSubscriptionService userSubscriptionService;

	@Mock
	private MobileIapVerificationService mobileIapVerificationService;

	@InjectMocks
	private SubscriptionService subscriptionService;

	@Test
	void getPlansCatalog_includesCurrencyAndThreePlans() {
		when(subscriptionProperties.getCurrency()).thenReturn("JPY");
		when(subscriptionProperties.stripeReady()).thenReturn(false);
		when(subscriptionProperties.appleIapConfigured()).thenReturn(false);
		when(subscriptionProperties.googlePlayConfigured()).thenReturn(false);
		when(subscriptionProperties.getPlusPriceMinor()).thenReturn(980);
		when(subscriptionProperties.getGoldPriceMinor()).thenReturn(1980);
		when(aiProperties.getGoldFairUseDailyCap()).thenReturn(500);
		when(aiProperties.quotasFor(any(SubscriptionPlan.class))).thenAnswer(inv -> {
			SubscriptionPlan p = inv.getArgument(0);
			if (p == SubscriptionPlan.FREE) {
				return new AiProperties.TierQuotas(3, 1, 1, 0);
			}
			if (p == SubscriptionPlan.PLUS) {
				return new AiProperties.TierQuotas(20, 5, 10, 20);
			}
			return new AiProperties.TierQuotas(-1, -1, -1, -1);
		});
		Map<String, Object> cat = subscriptionService.getPlansCatalog();
		assertEquals("jpy", cat.get("currency"));
		assertEquals(3, ((List<?>) cat.get("plans")).size());
		assertEquals(false, cat.get("stripeConfigured"));
	}

	@Test
	void currentPlan_delegatesToUserSubscriptionService() {
		ProfileEntity p = new ProfileEntity();
		when(profileRepo.findById(42L)).thenReturn(Optional.of(p));
		Map<String, Object> view = Map.of("tier", "PLUS");
		when(userSubscriptionService.currentBillingView(42L, p)).thenReturn(view);
		assertEquals(view, subscriptionService.currentPlan(42L));
		verify(userSubscriptionService).currentBillingView(42L, p);
	}

	@Test
	void createPaymentSession_rejectsFreePlan() {
		assertThrows(ResponseStatusException.class,
				() -> subscriptionService.createPaymentSession(1L, SubscriptionPlan.FREE));
	}
}