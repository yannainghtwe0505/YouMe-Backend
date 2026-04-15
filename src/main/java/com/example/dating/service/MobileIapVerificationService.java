package com.example.dating.service;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.config.SubscriptionProperties;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.UserSubscriptionRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Service
public class MobileIapVerificationService {

	public record VerifiedPurchase(SubscriptionPlan plan, String externalSubscriptionId, Instant expiresAt,
			String auditRef) {
	}

	private static final String APPLE_PROD = "https://buy.itunes.apple.com/verifyReceipt";
	private static final String APPLE_SANDBOX = "https://sandbox.itunes.apple.com/verifyReceipt";

	private final SubscriptionProperties subscriptionProperties;
	private final ObjectMapper objectMapper;
	private final UserSubscriptionRepo userSubscriptionRepo;
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private volatile AndroidPublisher cachedAndroidPublisher;

	public MobileIapVerificationService(SubscriptionProperties subscriptionProperties, ObjectMapper objectMapper,
			UserSubscriptionRepo userSubscriptionRepo) {
		this.subscriptionProperties = subscriptionProperties;
		this.objectMapper = objectMapper;
		this.userSubscriptionRepo = userSubscriptionRepo;
	}

	public VerifiedPurchase verifyIos(long userId, String receiptBase64) {
		if (!subscriptionProperties.appleIapConfigured())
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Apple IAP is not configured on server");
		if (receiptBase64 == null || receiptBase64.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiptBase64 required");
		try {
			JsonNode root = postApple(receiptBase64, false);
			int st = root.path("status").asInt(-1);
			if (st == 21007)
				root = postApple(receiptBase64, true);
			else if (st != 0)
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apple verifyReceipt status: " + st);

			JsonNode latest = root.path("latest_receipt_info");
			if (!latest.isArray() || latest.isEmpty())
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No subscription info in receipt");

			JsonNode best = null;
			long bestExp = -1;
			for (JsonNode n : latest) {
				String pid = n.path("product_id").asText("");
				SubscriptionPlan p = mapAppleProduct(pid);
				if (p == SubscriptionPlan.FREE)
					continue;
				long exp = parseAppleMillis(n.path("expires_date_ms").asText(null));
				if (exp > bestExp) {
					bestExp = exp;
					best = n;
				}
			}
			if (best == null)
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No recognized subscription product in receipt");

			String productId = best.path("product_id").asText("");
			String originalTx = best.path("original_transaction_id").asText("");
			if (originalTx.isBlank())
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing original_transaction_id");

			assertOriginalTransactionOwnership(userId, originalTx);

			SubscriptionPlan plan = mapAppleProduct(productId);
			Instant exp = Instant.ofEpochMilli(bestExp);
			if (Instant.now().isAfter(exp))
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription expired");

			String audit = "apple:" + originalTx + ":" + productId;
			return new VerifiedPurchase(plan, originalTx, exp, audit);
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple verification failed: " + e.getMessage());
		}
	}

	public VerifiedPurchase verifyAndroid(long userId, String productId, String purchaseToken) {
		if (!subscriptionProperties.googlePlayConfigured())
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google Play billing is not configured on server");
		if (productId == null || productId.isBlank() || purchaseToken == null || purchaseToken.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and purchaseToken required");

		SubscriptionPlan plan = mapGoogleProduct(productId);
		if (plan == SubscriptionPlan.FREE)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown productId");

		try {
			AndroidPublisher pub = androidPublisher();
			var sub = pub.purchases().subscriptionsv2()
					.get(subscriptionProperties.getGooglePlayPackageName(), purchaseToken)
					.execute();
			if (sub == null)
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty Play response");

			/* SubscriptionPurchaseV2: line items with expiry */
			String externalId = "gp:" + sha256Hex(purchaseToken);
			Instant exp = extractPlayExpiry(sub);
			if (exp != null && Instant.now().isAfter(exp))
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription expired");

			assertPurchaseTokenNotLinkedToOtherUser(userId, externalId);

			String audit = "google:" + productId + ":" + externalId;
			return new VerifiedPurchase(plan, externalId, exp, audit);
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Play verification failed: " + e.getMessage());
		}
	}

	private Instant extractPlayExpiry(com.google.api.services.androidpublisher.model.SubscriptionPurchaseV2 sub) {
		try {
			if (sub.getLineItems() == null)
				return null;
			Instant latest = null;
			for (var li : sub.getLineItems()) {
				if (li.getExpiryTime() == null)
					continue;
				Instant t = Instant.parse(li.getExpiryTime());
				if (latest == null || t.isAfter(latest))
					latest = t;
			}
			return latest;
		} catch (Exception e) {
			return null;
		}
	}

	private AndroidPublisher androidPublisher() throws Exception {
		if (cachedAndroidPublisher != null)
			return cachedAndroidPublisher;
		synchronized (this) {
			if (cachedAndroidPublisher != null)
				return cachedAndroidPublisher;
			try (FileInputStream in = new FileInputStream(subscriptionProperties.getGooglePlayCredentialsJson())) {
				GoogleCredentials creds = GoogleCredentials.fromStream(in)
						.createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));
				var transport = GoogleNetHttpTransport.newTrustedTransport();
				cachedAndroidPublisher = new AndroidPublisher.Builder(transport, GsonFactory.getDefaultInstance(),
						new HttpCredentialsAdapter(creds)).setApplicationName("YouMe").build();
				return cachedAndroidPublisher;
			}
		}
	}

	private void assertOriginalTransactionOwnership(long userId, String originalTx) {
		Optional<Long> other = userSubscriptionRepo.findByExternalSubscriptionId(originalTx).map(e -> e.getUserId());
		if (other.isPresent() && !other.get().equals(userId))
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This Apple subscription is already linked to another account.");
	}

	private void assertPurchaseTokenNotLinkedToOtherUser(long userId, String externalId) {
		Optional<Long> other = userSubscriptionRepo.findByExternalSubscriptionId(externalId).map(e -> e.getUserId());
		if (other.isPresent() && !other.get().equals(userId))
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This Play purchase is already linked to another account.");
	}

	private JsonNode postApple(String receiptBase64, boolean sandbox) throws Exception {
		var body = objectMapper.createObjectNode();
		body.put("receipt-data", receiptBase64);
		body.put("password", subscriptionProperties.getAppleSharedSecret());
		body.put("exclude-old-transactions", true);
		String url = sandbox ? APPLE_SANDBOX : APPLE_PROD;
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() / 100 != 2)
			throw new IllegalStateException("HTTP " + res.statusCode());
		return objectMapper.readTree(res.body());
	}

	private static long parseAppleMillis(String raw) {
		if (raw == null)
			return -1;
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private SubscriptionPlan mapAppleProduct(String productId) {
		if (productId.equals(subscriptionProperties.getAppleProductGoldMonthly()))
			return SubscriptionPlan.GOLD;
		if (productId.equals(subscriptionProperties.getAppleProductPlusMonthly()))
			return SubscriptionPlan.PLUS;
		return SubscriptionPlan.FREE;
	}

	private SubscriptionPlan mapGoogleProduct(String productId) {
		if (productId.equals(subscriptionProperties.getGoogleProductGoldMonthly()))
			return SubscriptionPlan.GOLD;
		if (productId.equals(subscriptionProperties.getGoogleProductPlusMonthly()))
			return SubscriptionPlan.PLUS;
		return SubscriptionPlan.FREE;
	}

	private static String sha256Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : d)
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
