package com.example.dating.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserSubscriptionEntity;
import com.example.dating.model.subscription.BillingProvider;
import com.example.dating.model.subscription.SubscriptionLifecycleStatus;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.UserSubscriptionRepo;

@Service
public class SubscriptionPlanService {

	private final UserSubscriptionRepo userSubscriptionRepo;

	public SubscriptionPlanService(UserSubscriptionRepo userSubscriptionRepo) {
		this.userSubscriptionRepo = userSubscriptionRepo;
	}

	/**
	 * Effective tier for entitlements. Uses {@code user_subscription} when present, otherwise legacy
	 * {@code profiles.subscription_plan} / {@code is_premium}.
	 */
	public SubscriptionPlan resolve(ProfileEntity p) {
		if (p == null)
			return SubscriptionPlan.FREE;
		Long userId = p.getUserId();
		if (userId != null) {
			return userSubscriptionRepo.findById(userId).map(this::effectivePlanFromBillingRow)
					.orElseGet(() -> resolveLegacy(p));
		}
		return resolveLegacy(p);
	}

	private SubscriptionPlan effectivePlanFromBillingRow(UserSubscriptionEntity us) {
		SubscriptionLifecycleStatus st = SubscriptionLifecycleStatus.fromDb(us.getLifecycleStatus());
		SubscriptionPlan tier = SubscriptionPlan.fromDb(us.getPlanTier());
		Instant end = us.getCurrentPeriodEnd();
		Instant now = Instant.now();

		if (st == SubscriptionLifecycleStatus.PENDING)
			return SubscriptionPlan.FREE;

		if (st == SubscriptionLifecycleStatus.NONE || tier == SubscriptionPlan.FREE)
			return SubscriptionPlan.FREE;

		if (st == SubscriptionLifecycleStatus.EXPIRED)
			return SubscriptionPlan.FREE;

		if (st == SubscriptionLifecycleStatus.CANCELED) {
			if (us.isCancelAtPeriodEnd() && end != null && now.isBefore(end))
				return tier;
			return SubscriptionPlan.FREE;
		}

		if (st == SubscriptionLifecycleStatus.ACTIVE) {
			if (end != null && now.isAfter(end))
				return SubscriptionPlan.FREE;
			return tier;
		}
		return tier;
	}

	/**
	 * Column {@code subscription_plan} wins; legacy {@code is_premium} maps to PLUS when plan is FREE.
	 */
	private SubscriptionPlan resolveLegacy(ProfileEntity p) {
		SubscriptionPlan fromCol = SubscriptionPlan.fromDb(p.getSubscriptionPlan());
		if (fromCol != SubscriptionPlan.FREE)
			return fromCol;
		if (p.isPremium())
			return SubscriptionPlan.PLUS;
		return SubscriptionPlan.FREE;
	}

	/** Keep legacy clients working: premium flag true for any paid tier. */
	public void applyPlanToProfile(ProfileEntity p, SubscriptionPlan plan) {
		p.setSubscriptionPlan(plan.name());
		p.setPremium(plan != SubscriptionPlan.FREE);
	}

	public BillingProvider billingProviderForUser(long userId) {
		return userSubscriptionRepo.findById(userId)
				.map(u -> BillingProvider.fromDb(u.getBillingProvider()))
				.orElse(BillingProvider.NONE);
	}
}
