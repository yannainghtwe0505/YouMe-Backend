package com.example.dating.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.config.AiProperties;
import com.example.dating.config.AppUrlsProperties;
import com.example.dating.config.SubscriptionProperties;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.BillingProvider;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class SubscriptionService {

	private final SubscriptionProperties subscriptionProperties;
	private final AppUrlsProperties appUrlsProperties;
	private final AiProperties aiProperties;
	private final ProfileRepo profileRepo;
	private final UserRepo userRepo;
	private final SubscriptionPlanService subscriptionPlanService;
	private final UserSubscriptionService userSubscriptionService;
	private final MobileIapVerificationService mobileIapVerificationService;

	public SubscriptionService(SubscriptionProperties subscriptionProperties, AppUrlsProperties appUrlsProperties,
			AiProperties aiProperties, ProfileRepo profileRepo, UserRepo userRepo,
			SubscriptionPlanService subscriptionPlanService, UserSubscriptionService userSubscriptionService,
			MobileIapVerificationService mobileIapVerificationService) {
		this.subscriptionProperties = subscriptionProperties;
		this.appUrlsProperties = appUrlsProperties;
		this.aiProperties = aiProperties;
		this.profileRepo = profileRepo;
		this.userRepo = userRepo;
		this.subscriptionPlanService = subscriptionPlanService;
		this.userSubscriptionService = userSubscriptionService;
		this.mobileIapVerificationService = mobileIapVerificationService;
	}

	public Map<String, Object> getPlansCatalog() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("billingCycle", "monthly");
		out.put("currency", subscriptionProperties.getCurrency().toLowerCase(Locale.ROOT));
		out.put("stripeConfigured", subscriptionProperties.stripeReady());
		out.put("appleConfigured", subscriptionProperties.appleIapConfigured());
		out.put("googleConfigured", subscriptionProperties.googlePlayConfigured());
		out.put("plans", List.of(planCard(SubscriptionPlan.FREE), planCard(SubscriptionPlan.PLUS),
				planCard(SubscriptionPlan.GOLD)));
		out.put("comparison", buildComparisonRows());
		return out;
	}

	private Map<String, Object> planCard(SubscriptionPlan plan) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", plan.name());
		int minor = switch (plan) {
			case FREE -> 0;
			case PLUS -> subscriptionProperties.getPlusPriceMinor();
			case GOLD -> subscriptionProperties.getGoldPriceMinor();
		};
		m.put("monthlyPriceMinor", minor);
		m.put("badge", switch (plan) {
			case PLUS -> "POPULAR";
			case GOLD -> "RECOMMENDED";
			case FREE -> null;
		});
		return m;
	}

	private List<Map<String, String>> buildComparisonRows() {
		var f = aiProperties.quotasFor(SubscriptionPlan.FREE);
		var p = aiProperties.quotasFor(SubscriptionPlan.PLUS);
		var g = aiProperties.quotasFor(SubscriptionPlan.GOLD);
		int cap = aiProperties.getGoldFairUseDailyCap();
		List<Map<String, String>> rows = new ArrayList<>();
		rows.add(row("aiReplies", f, p, g, AiFeature.CHAT_REPLY, cap));
		rows.add(row("profileAi", f, p, g, AiFeature.PROFILE_AI, cap));
		rows.add(row("matchGreetings", f, p, g, AiFeature.MATCH_GREETING, cap));
		rows.add(row("chatInsights", f, p, g, AiFeature.MATCH_INSIGHT, cap));
		rows.add(smartMatchingRow());
		rows.add(dailyQuotaSummaryRow(f, p, g, cap));
		return rows;
	}

	private Map<String, String> row(String id, AiProperties.TierQuotas f, AiProperties.TierQuotas p,
			AiProperties.TierQuotas g, AiFeature feature, int fairCap) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("id", id);
		m.put("free", formatQuota(f.limit(feature), fairCap));
		m.put("plus", formatQuota(p.limit(feature), fairCap));
		m.put("gold", formatQuota(g.limit(feature), fairCap));
		return m;
	}

	private Map<String, String> smartMatchingRow() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("id", "smartMatching");
		m.put("free", "Standard");
		m.put("plus", "Smart");
		m.put("gold", "Priority + deeper signals");
		return m;
	}

	private Map<String, String> dailyQuotaSummaryRow(AiProperties.TierQuotas f, AiProperties.TierQuotas p,
			AiProperties.TierQuotas g, int fairCap) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("id", "dailyAiQuota");
		m.put("free", formatQuota(f.limit(AiFeature.CHAT_REPLY), fairCap));
		m.put("plus", formatQuota(p.limit(AiFeature.CHAT_REPLY), fairCap));
		m.put("gold", formatQuota(g.limit(AiFeature.CHAT_REPLY), fairCap));
		return m;
	}

	private String formatQuota(int limit, int fairCap) {
		if (limit < 0)
			return fairCap > 0 ? "Up to " + fairCap + "/day (fair use)" : "Unlimited";
		if (limit == 0)
			return "—";
		return limit + "/day";
	}

	public Map<String, Object> currentPlan(long userId) {
		ProfileEntity p = profileRepo.findById(userId).orElse(null);
		return userSubscriptionService.currentBillingView(userId, p);
	}

	public Map<String, Object> createPaymentSession(long userId, SubscriptionPlan targetPlan) {
		if (targetPlan != SubscriptionPlan.PLUS && targetPlan != SubscriptionPlan.GOLD)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target plan");
		ProfileEntity profile = profileRepo.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
		SubscriptionPlan current = subscriptionPlanService.resolve(profile);
		if (current == targetPlan)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already on this plan");
		if (!current.isStrictlyLowerThan(targetPlan))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change to this plan");

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("billingCycle", "monthly");
		body.put("targetPlan", targetPlan.name());
		body.put("monthlyPriceMinor",
				targetPlan == SubscriptionPlan.PLUS ? subscriptionProperties.getPlusPriceMinor()
						: subscriptionProperties.getGoldPriceMinor());
		body.put("currency", subscriptionProperties.getCurrency().toLowerCase(Locale.ROOT));

		if (!subscriptionProperties.stripeReady()) {
			body.put("stripeConfigured", false);
			body.put("checkoutUrl", null);
			body.put("demoUpgradeAvailable", true);
			return body;
		}

		String priceId = targetPlan == SubscriptionPlan.PLUS ? subscriptionProperties.getStripePricePlusMonthly()
				: subscriptionProperties.getStripePriceGoldMonthly();
		String email = userRepo.findById(userId).map(u -> u.getEmail()).orElse(null);

		try {
			Stripe.apiKey = subscriptionProperties.getSecretKey();
			String base = appUrlsProperties.getFrontendBaseUrl().replaceAll("/$", "");
			String successUrl = base + subscriptionProperties.getSuccessPath() + "?session_id={CHECKOUT_SESSION_ID}";
			String cancelUrl = base + subscriptionProperties.getCancelPath();

			SessionCreateParams.Builder b = SessionCreateParams.builder()
					.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
					.setClientReferenceId(Long.toString(userId))
					.putMetadata("userId", Long.toString(userId))
					.putMetadata("targetPlan", targetPlan.name())
					.addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
					.setSuccessUrl(successUrl)
					.setCancelUrl(cancelUrl);
			if (email != null && !email.isBlank())
				b.setCustomerEmail(email);
			Session session = Session.create(b.build());
			body.put("stripeConfigured", true);
			body.put("checkoutUrl", session.getUrl());
			return body;
		} catch (StripeException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
		}
	}

	@Transactional
	public Map<String, Object> confirmCheckoutSession(long userId, String sessionId) {
		if (sessionId == null || sessionId.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId required");
		if (!subscriptionProperties.stripeReady())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe is not configured");
		try {
			Stripe.apiKey = subscriptionProperties.getSecretKey();
			Session session = Session.retrieve(sessionId);
			Map<String, String> meta = session.getMetadata();
			if (meta == null || !Long.toString(userId).equals(meta.get("userId")))
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session does not belong to this user");
			String pay = session.getPaymentStatus();
			if (pay == null
					|| (!"paid".equalsIgnoreCase(pay) && !"no_payment_required".equalsIgnoreCase(pay)))
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment not completed");
			userSubscriptionService.applyStripeCheckoutSession(session);
			ProfileEntity updated = profileRepo.findById(userId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
			SubscriptionPlan applied = subscriptionPlanService.resolve(updated);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("ok", true);
			out.put("plan", applied.name());
			return out;
		} catch (StripeException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
		}
	}

	public String processStripeWebhookEvent(Event event) {
		StripeObject obj = event.getDataObjectDeserializer().getObject().orElse(null);
		switch (event.getType()) {
			case "checkout.session.completed":
				if (obj instanceof Session session)
					userSubscriptionService.applyStripeCheckoutSession(session);
				return "ok";
			case "customer.subscription.updated":
			case "customer.subscription.deleted":
				if (obj instanceof Subscription sub)
					userSubscriptionService.applyStripeSubscriptionEvent(sub);
				return "ok";
			default:
				return "ignored";
		}
	}

	public String handleStripeWebhook(String payload, String sigHeader) {
		if (sigHeader == null || sigHeader.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe-Signature");
		if (subscriptionProperties.getWebhookSecret() == null
				|| subscriptionProperties.getWebhookSecret().isBlank())
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook not configured");
		final Event event;
		try {
			event = Webhook.constructEvent(payload, sigHeader, subscriptionProperties.getWebhookSecret());
		} catch (SignatureVerificationException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
		}
		return processStripeWebhookEvent(event);
	}

	public Map<String, Object> verifyMobilePurchase(long userId, String platform, String receiptBase64,
			String productId, String purchaseToken) {
		if (platform == null || platform.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform required (ios or android)");
		String p = platform.trim().toLowerCase(Locale.ROOT);
		MobileIapVerificationService.VerifiedPurchase v;
		if ("ios".equals(p) || "apple".equals(p)) {
			v = mobileIapVerificationService.verifyIos(userId, receiptBase64);
			userSubscriptionService.applyMobileEntitlement(userId, v.plan(), BillingProvider.APPLE,
					v.externalSubscriptionId(), v.auditRef(), v.expiresAt());
		} else if ("android".equals(p) || "google".equals(p)) {
			v = mobileIapVerificationService.verifyAndroid(userId, productId, purchaseToken);
			userSubscriptionService.applyMobileEntitlement(userId, v.plan(), BillingProvider.GOOGLE,
					v.externalSubscriptionId(), v.auditRef(), v.expiresAt());
		} else
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform must be ios or android");
		ProfileEntity profile = profileRepo.findById(userId).orElse(null);
		SubscriptionPlan effective = subscriptionPlanService.resolve(profile);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("ok", true);
		out.put("plan", effective.name());
		out.put("expiresAt", v.expiresAt() != null ? v.expiresAt().toString() : null);
		return out;
	}
}
