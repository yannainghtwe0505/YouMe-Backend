package com.example.dating.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.SubscriptionService;

/**
 * Provider webhooks (Stripe today; Apple ASN v2 / Google RTDN wired as production hardening).
 */
@RestController
@RequestMapping("/webhooks")
public class BillingWebhookController {

	private final SubscriptionService subscriptionService;

	public BillingWebhookController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@PostMapping(value = "/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature) {
		String result = subscriptionService.handleStripeWebhook(payload, stripeSignature);
		return ResponseEntity.ok(result);
	}

	/**
	 * Apple App Store Server Notifications v2 (JWT). Placeholder: verify JWS and map
	 * notificationType/subtype to {@link com.example.dating.service.UserSubscriptionService}.
	 */
	@PostMapping(value = "/apple", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> appleWebhook(@RequestBody(required = false) String body) {
		return ResponseEntity.ok(Map.of(
				"status", "accepted",
				"note", "Implement ASN v2 verification and entitlement sync (see docs/SYSTEM_SPECIFICATION.md)."));
	}

	/**
	 * Google Play Real-time Developer Notifications (often delivered via Pub/Sub push). Placeholder.
	 */
	@PostMapping(value = "/google", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> googleWebhook(@RequestBody(required = false) String body) {
		return ResponseEntity.ok(Map.of(
				"status", "accepted",
				"note", "Subscribe to Pub/Sub and verify notifications; see docs/SYSTEM_SPECIFICATION.md."));
	}
}
