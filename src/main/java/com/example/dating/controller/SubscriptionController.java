package com.example.dating.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.service.SubscriptionService;
import com.example.dating.service.UserSubscriptionService;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;
	private final UserSubscriptionService userSubscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService,
			UserSubscriptionService userSubscriptionService) {
		this.subscriptionService = subscriptionService;
		this.userSubscriptionService = userSubscriptionService;
	}

	@GetMapping("/plans")
	public Map<String, Object> plans() {
		return subscriptionService.getPlansCatalog();
	}

	@GetMapping("/current")
	public Map<String, Object> current(@AuthenticationPrincipal User me) {
		long userId = Long.parseLong(me.getUsername());
		return subscriptionService.currentPlan(userId);
	}

	@PostMapping("/web/checkout-session")
	public Map<String, Object> webCheckoutSession(@AuthenticationPrincipal User me,
			@RequestBody CreatePaymentBody body) {
		long userId = Long.parseLong(me.getUsername());
		if (body == null || body.targetPlan == null || body.targetPlan.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetPlan required");
		SubscriptionPlan target = SubscriptionPlan.fromDb(body.targetPlan);
		return subscriptionService.createPaymentSession(userId, target);
	}

	@PostMapping("/mobile/verify-purchase")
	public Map<String, Object> verifyPurchase(@AuthenticationPrincipal User me,
			@RequestBody MobileVerifyBody body) {
		long userId = Long.parseLong(me.getUsername());
		if (body == null || body.platform == null || body.platform.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform required");
		return subscriptionService.verifyMobilePurchase(userId, body.platform, body.receiptData, body.productId,
				body.purchaseToken);
	}

	@PostMapping("/restore-purchase")
	public Map<String, Object> restorePurchase(@AuthenticationPrincipal User me,
			@RequestBody MobileVerifyBody body) {
		return verifyPurchase(me, body);
	}

	@PostMapping("/cancel")
	public Map<String, Object> cancel(@AuthenticationPrincipal User me, @RequestBody(required = false) CancelBody body) {
		long userId = Long.parseLong(me.getUsername());
		boolean immediate = body != null && body.immediate;
		return userSubscriptionService.cancel(userId, immediate);
	}

	@PostMapping("/downgrade")
	public Map<String, Object> downgrade(@AuthenticationPrincipal User me, @RequestBody DowngradeBody body) {
		long userId = Long.parseLong(me.getUsername());
		if (body == null || body.targetPlan == null || body.targetPlan.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetPlan required");
		if (body.effective != null && body.effective.equalsIgnoreCase("PERIOD_END"))
			throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
					"Period-end downgrade is not implemented yet; use cancel at period end or contact support.");
		SubscriptionPlan target = SubscriptionPlan.fromDb(body.targetPlan);
		return userSubscriptionService.downgrade(userId, target, true);
	}

	/** @deprecated Use {@code POST /subscription/web/checkout-session} */
	@PostMapping("/create-payment-session")
	public Map<String, Object> createPaymentSessionLegacy(@AuthenticationPrincipal User me,
			@RequestBody CreatePaymentBody body) {
		return webCheckoutSession(me, body);
	}

	/** @deprecated Use {@code POST /webhooks/stripe} */
	@PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> webhookLegacy(@RequestBody String payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature) {
		String result = subscriptionService.handleStripeWebhook(payload, stripeSignature);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/upgrade-confirm")
	public Map<String, Object> upgradeConfirm(@AuthenticationPrincipal User me, @RequestBody SessionIdBody body) {
		long userId = Long.parseLong(me.getUsername());
		if (body == null || body.sessionId == null || body.sessionId.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId required");
		return subscriptionService.confirmCheckoutSession(userId, body.sessionId.trim());
	}

	public static class CreatePaymentBody {
		public String targetPlan;
	}

	public static class SessionIdBody {
		public String sessionId;
	}

	public static class MobileVerifyBody {
		/** "ios" | "android" */
		public String platform;
		/** Apple: base64 receipt; omit for Google. */
		public String receiptData;
		/** Google: Play product id (SKU). */
		public String productId;
		/** Google: purchase token from BillingClient. */
		public String purchaseToken;
	}

	public static class CancelBody {
		/** When true, end access immediately (Stripe cancel now). */
		public boolean immediate;
	}

	public static class DowngradeBody {
		public String targetPlan;
		/** IMMEDIATE (default) or PERIOD_END (not implemented for Stripe). */
		public String effective;
	}
}
