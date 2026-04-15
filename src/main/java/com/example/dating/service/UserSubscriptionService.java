package com.example.dating.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.config.SubscriptionProperties;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserSubscriptionEntity;
import com.example.dating.model.subscription.BillingProvider;
import com.example.dating.model.subscription.SubscriptionLifecycleStatus;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserSubscriptionRepo;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;

@Service
public class UserSubscriptionService {

	private final UserSubscriptionRepo userSubscriptionRepo;
	private final ProfileRepo profileRepo;
	private final SubscriptionPlanService subscriptionPlanService;
	private final SubscriptionProperties subscriptionProperties;

	public UserSubscriptionService(UserSubscriptionRepo userSubscriptionRepo, ProfileRepo profileRepo,
			SubscriptionPlanService subscriptionPlanService, SubscriptionProperties subscriptionProperties) {
		this.userSubscriptionRepo = userSubscriptionRepo;
		this.profileRepo = profileRepo;
		this.subscriptionPlanService = subscriptionPlanService;
		this.subscriptionProperties = subscriptionProperties;
	}

	@Transactional
	public UserSubscriptionEntity ensureRow(long userId) {
		return userSubscriptionRepo.findById(userId).orElseGet(() -> {
			UserSubscriptionEntity e = new UserSubscriptionEntity();
			e.setUserId(userId);
			e.setPlanTier(SubscriptionPlan.FREE.name());
			e.setBillingProvider(BillingProvider.NONE.name());
			e.setLifecycleStatus(SubscriptionLifecycleStatus.NONE.name());
			return userSubscriptionRepo.save(e);
		});
	}

	@Transactional
	public void syncProfileFromBilling(long userId) {
		ProfileEntity p = profileRepo.findById(userId).orElse(null);
		if (p == null)
			return;
		SubscriptionPlan effective = subscriptionPlanService.resolve(p);
		subscriptionPlanService.applyPlanToProfile(p, effective);
		profileRepo.save(p);
	}

	@Transactional
	public void applyStripeCheckoutSession(Session session) {
		Map<String, String> meta = session.getMetadata();
		if (meta == null)
			return;
		String uid = meta.get("userId");
		String planRaw = meta.get("targetPlan");
		if (uid == null || planRaw == null)
			return;
		long userId;
		try {
			userId = Long.parseLong(uid);
		} catch (NumberFormatException e) {
			return;
		}
		SubscriptionPlan target = SubscriptionPlan.fromDb(planRaw);
		if (target != SubscriptionPlan.PLUS && target != SubscriptionPlan.GOLD)
			return;
		ProfileEntity profile = profileRepo.findById(userId).orElse(null);
		if (profile == null)
			return;
		UserSubscriptionEntity row = ensureRow(userId);
		String sessionSubId = session.getSubscription();
		if (sessionSubId != null && sessionSubId.equals(row.getExternalSubscriptionId())) {
			try {
				Stripe.apiKey = subscriptionProperties.getSecretKey();
				Subscription sub = Subscription.retrieve(sessionSubId);
				applyStripeSubscriptionFields(row, sub);
				if (session.getCustomer() != null)
					profile.setStripeCustomerId(session.getCustomer());
				profile.setStripeSubscriptionId(sessionSubId);
				userSubscriptionRepo.save(row);
				profileRepo.save(profile);
				syncProfileFromBilling(userId);
			} catch (StripeException ignored) {
				/* ignore */
			}
			return;
		}
		SubscriptionPlan current = subscriptionPlanService.resolve(profile);
		if (!current.isStrictlyLowerThan(target))
			return;

		row.setBillingProvider(BillingProvider.STRIPE.name());
		row.setPlanTier(target.name());
		row.setLifecycleStatus(SubscriptionLifecycleStatus.ACTIVE.name());
		row.setCancelAtPeriodEnd(false);
		if (session.getSubscription() != null) {
			row.setExternalSubscriptionId(session.getSubscription());
			try {
				Stripe.apiKey = subscriptionProperties.getSecretKey();
				Subscription sub = Subscription.retrieve(session.getSubscription());
				applyStripeSubscriptionFields(row, sub);
			} catch (StripeException ignored) {
				row.setCurrentPeriodEnd(null);
			}
		}
		if (session.getCustomer() != null)
			profile.setStripeCustomerId(session.getCustomer());
		if (session.getSubscription() != null)
			profile.setStripeSubscriptionId(session.getSubscription());
		String receiptSnap = "stripe:session:" + session.getId();
		if (receiptSnap.length() > 8000)
			receiptSnap = receiptSnap.substring(0, 8000);
		row.setReceiptData(receiptSnap);
		userSubscriptionRepo.save(row);
		profileRepo.save(profile);
		syncProfileFromBilling(userId);
	}

	@Transactional
	public void applyStripeSubscriptionEvent(Subscription sub) {
		if (sub == null || sub.getId() == null)
			return;
		Optional<UserSubscriptionEntity> opt = userSubscriptionRepo.findByExternalSubscriptionId(sub.getId());
		if (opt.isEmpty())
			return;
		UserSubscriptionEntity row = opt.get();
		String st = sub.getStatus();
		if ("canceled".equalsIgnoreCase(st) || "unpaid".equalsIgnoreCase(st)) {
			row.setLifecycleStatus(SubscriptionLifecycleStatus.EXPIRED.name());
			row.setPlanTier(SubscriptionPlan.FREE.name());
			row.setCancelAtPeriodEnd(false);
		} else {
			applyStripeSubscriptionFields(row, sub);
			if (Boolean.TRUE.equals(sub.getCancelAtPeriodEnd())) {
				row.setLifecycleStatus(SubscriptionLifecycleStatus.CANCELED.name());
				row.setCancelAtPeriodEnd(true);
			} else {
				row.setLifecycleStatus(SubscriptionLifecycleStatus.ACTIVE.name());
				row.setCancelAtPeriodEnd(false);
			}
		}
		userSubscriptionRepo.save(row);
		syncProfileFromBilling(row.getUserId());
	}

	private void applyStripeSubscriptionFields(UserSubscriptionEntity row, Subscription sub) {
		Long cpe = sub.getCurrentPeriodEnd();
		if (cpe != null)
			row.setCurrentPeriodEnd(Instant.ofEpochSecond(cpe));
		String priceId = null;
		try {
			if (sub.getItems() != null && sub.getItems().getData() != null && !sub.getItems().getData().isEmpty()) {
				var item = sub.getItems().getData().get(0);
				if (item.getPrice() != null)
					priceId = item.getPrice().getId();
			}
		} catch (Exception ignored) {
			/* best-effort */
		}
		if (priceId != null) {
			SubscriptionPlan fromPrice = mapStripePriceIdToPlan(priceId);
			if (fromPrice != SubscriptionPlan.FREE)
				row.setPlanTier(fromPrice.name());
		}
	}

	private SubscriptionPlan mapStripePriceIdToPlan(String priceId) {
		if (priceId == null)
			return SubscriptionPlan.FREE;
		if (priceId.equals(subscriptionProperties.getStripePriceGoldMonthly()))
			return SubscriptionPlan.GOLD;
		if (priceId.equals(subscriptionProperties.getStripePricePlusMonthly()))
			return SubscriptionPlan.PLUS;
		return SubscriptionPlan.PLUS;
	}

	@Transactional
	public Map<String, Object> cancel(long userId, boolean immediate) {
		UserSubscriptionEntity row = ensureRow(userId);
		BillingProvider p = BillingProvider.fromDb(row.getBillingProvider());
		SubscriptionLifecycleStatus st = SubscriptionLifecycleStatus.fromDb(row.getLifecycleStatus());
		SubscriptionPlan tier = SubscriptionPlan.fromDb(row.getPlanTier());
		/* Dev/demo tier (POST /me/upgrade) — no external billing provider */
		if (p == BillingProvider.NONE && tier != SubscriptionPlan.FREE && st == SubscriptionLifecycleStatus.ACTIVE) {
			row.setPlanTier(SubscriptionPlan.FREE.name());
			row.setLifecycleStatus(SubscriptionLifecycleStatus.NONE.name());
			row.setCancelAtPeriodEnd(false);
			row.setCurrentPeriodEnd(null);
			row.setExternalSubscriptionId(null);
			row.setReceiptData(null);
			userSubscriptionRepo.save(row);
			syncProfileFromBilling(userId);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("ok", true);
			out.put("lifecycleStatus", row.getLifecycleStatus());
			out.put("cancelAtPeriodEnd", false);
			out.put("plan", SubscriptionPlan.FREE.name());
			return out;
		}
		if (p == BillingProvider.NONE || st == SubscriptionLifecycleStatus.NONE)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active paid subscription");
		Map<String, Object> out = new LinkedHashMap<>();
		if (p == BillingProvider.STRIPE) {
			if (!subscriptionProperties.stripeReady())
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe is not configured");
			String subId = row.getExternalSubscriptionId();
			if (subId == null || subId.isBlank())
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe subscription id");
			try {
				Stripe.apiKey = subscriptionProperties.getSecretKey();
				Subscription sub = Subscription.retrieve(subId);
				if (immediate) {
					sub.cancel();
					row.setLifecycleStatus(SubscriptionLifecycleStatus.EXPIRED.name());
					row.setPlanTier(SubscriptionPlan.FREE.name());
					row.setCancelAtPeriodEnd(false);
					row.setCurrentPeriodEnd(Instant.now());
				} else {
					sub.update(SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build());
					row.setCancelAtPeriodEnd(true);
					row.setLifecycleStatus(SubscriptionLifecycleStatus.CANCELED.name());
				}
			} catch (StripeException e) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
			}
		} else {
			/* Apple / Google: OS manages cancellation; we only mirror intent for UX sync. */
			row.setCancelAtPeriodEnd(!immediate);
			row.setLifecycleStatus(immediate ? SubscriptionLifecycleStatus.EXPIRED.name()
					: SubscriptionLifecycleStatus.CANCELED.name());
			if (immediate) {
				row.setPlanTier(SubscriptionPlan.FREE.name());
				row.setCurrentPeriodEnd(Instant.now());
			}
		}
		userSubscriptionRepo.save(row);
		syncProfileFromBilling(userId);
		out.put("ok", true);
		out.put("lifecycleStatus", row.getLifecycleStatus());
		out.put("cancelAtPeriodEnd", row.isCancelAtPeriodEnd());
		out.put("plan", subscriptionPlanService.resolve(profileRepo.findById(userId).orElse(null)).name());
		return out;
	}

	@Transactional
	public Map<String, Object> downgrade(long userId, SubscriptionPlan target, boolean immediate) {
		if (target != SubscriptionPlan.FREE && target != SubscriptionPlan.PLUS)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid downgrade target");
		UserSubscriptionEntity row = ensureRow(userId);
		BillingProvider p = BillingProvider.fromDb(row.getBillingProvider());
		SubscriptionPlan storedTier = SubscriptionPlan.fromDb(row.getPlanTier());

		if (p != BillingProvider.STRIPE)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Downgrade for store subscriptions must be managed in Apple/Google subscription settings, or cancel and resubscribe.");

		if (!immediate)
			throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
					"Period-end downgrade scheduling is not implemented yet; use cancel_at_period_end or contact support.");

		if (!subscriptionProperties.stripeReady())
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe is not configured");

		if (target == SubscriptionPlan.FREE)
			return cancel(userId, true);

		/* PLUS target: only GOLD → PLUS */
		if (storedTier != SubscriptionPlan.GOLD)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Downgrade to Plus is only available from Gold");

		String subId = row.getExternalSubscriptionId();
		if (subId == null || subId.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe subscription id");
		String newPriceId = subscriptionProperties.getStripePricePlusMonthly();
		try {
			Stripe.apiKey = subscriptionProperties.getSecretKey();
			Subscription sub = Subscription.retrieve(subId);
			String itemId = sub.getItems().getData().get(0).getId();
			var item = SubscriptionUpdateParams.Item.builder().setId(itemId).setPrice(newPriceId).build();
			sub.update(SubscriptionUpdateParams.builder().addItem(item)
					.setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE).build());
			row.setPlanTier(SubscriptionPlan.PLUS.name());
			row.setLifecycleStatus(SubscriptionLifecycleStatus.ACTIVE.name());
			userSubscriptionRepo.save(row);
			syncProfileFromBilling(userId);
			return Map.of("ok", true, "plan", SubscriptionPlan.PLUS.name());
		} catch (StripeException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
		}
	}

	@Transactional
	public void applyMobileEntitlement(long userId, SubscriptionPlan tier, BillingProvider provider,
			String externalId, String receiptSnapshot, Instant periodEnd) {
		assertNoActiveStripeConflict(userId, provider);
		UserSubscriptionEntity row = ensureRow(userId);
		row.setBillingProvider(provider.name());
		row.setPlanTier(tier.name());
		row.setLifecycleStatus(SubscriptionLifecycleStatus.ACTIVE.name());
		row.setExternalSubscriptionId(externalId);
		row.setCancelAtPeriodEnd(false);
		row.setCurrentPeriodEnd(periodEnd);
		if (receiptSnapshot != null && receiptSnapshot.length() > 12000)
			receiptSnapshot = receiptSnapshot.substring(0, 12000);
		row.setReceiptData(receiptSnapshot);
		userSubscriptionRepo.save(row);
		syncProfileFromBilling(userId);
	}

	public void assertNoActiveStripeConflict(long userId, BillingProvider incoming) {
		if (incoming == BillingProvider.STRIPE)
			return;
		UserSubscriptionEntity row = userSubscriptionRepo.findById(userId).orElse(null);
		if (row == null)
			return;
		BillingProvider cur = BillingProvider.fromDb(row.getBillingProvider());
		if (cur != BillingProvider.STRIPE)
			return;
		SubscriptionLifecycleStatus st = SubscriptionLifecycleStatus.fromDb(row.getLifecycleStatus());
		if (st != SubscriptionLifecycleStatus.ACTIVE && st != SubscriptionLifecycleStatus.CANCELED)
			return;
		if (st == SubscriptionLifecycleStatus.CANCELED && row.getCurrentPeriodEnd() != null
				&& Instant.now().isAfter(row.getCurrentPeriodEnd()))
			return;
		throw new ResponseStatusException(HttpStatus.CONFLICT,
				"An active Stripe subscription is linked to this account. Cancel it on the web before using in-app purchases.");
	}

	@Transactional
	public void applyDevDemoPlan(long userId, SubscriptionPlan plan) {
		if (plan != SubscriptionPlan.PLUS && plan != SubscriptionPlan.GOLD)
			return;
		UserSubscriptionEntity row = ensureRow(userId);
		row.setBillingProvider(BillingProvider.NONE.name());
		row.setExternalSubscriptionId(null);
		row.setPlanTier(plan.name());
		row.setLifecycleStatus(SubscriptionLifecycleStatus.ACTIVE.name());
		row.setCancelAtPeriodEnd(false);
		row.setCurrentPeriodEnd(null);
		row.setReceiptData("demo:me/upgrade");
		userSubscriptionRepo.save(row);
		syncProfileFromBilling(userId);
	}

	public Map<String, Object> currentBillingView(long userId, ProfileEntity profile) {
		UserSubscriptionEntity row = userSubscriptionRepo.findById(userId).orElse(null);
		SubscriptionPlan effective = subscriptionPlanService.resolve(profile);
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("plan", effective.name());
		m.put("isPremium", effective != SubscriptionPlan.FREE);
		if (row != null) {
			m.put("billingProvider", row.getBillingProvider());
			m.put("lifecycleStatus", row.getLifecycleStatus());
			m.put("currentPeriodEnd", row.getCurrentPeriodEnd() != null ? row.getCurrentPeriodEnd().toString() : null);
			m.put("cancelAtPeriodEnd", row.isCancelAtPeriodEnd());
			m.put("planTierStored", row.getPlanTier());
		} else {
			m.put("billingProvider", BillingProvider.NONE.name());
			m.put("lifecycleStatus", SubscriptionLifecycleStatus.NONE.name());
			m.put("currentPeriodEnd", null);
			m.put("cancelAtPeriodEnd", false);
			m.put("planTierStored", effective.name());
		}
		m.put("webCheckoutAvailable", subscriptionProperties.stripeReady());
		return m;
	}
}
