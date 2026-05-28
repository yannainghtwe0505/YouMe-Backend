package com.example.dating.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.dating.config.PushProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

@Service
public class WebPushNotificationSender {
	private static final Logger log = LoggerFactory.getLogger(WebPushNotificationSender.class);

	private final PushProperties pushProperties;
	private final ObjectMapper objectMapper;
	private volatile PushService pushService;

	public WebPushNotificationSender(PushProperties pushProperties, ObjectMapper objectMapper) {
		this.pushProperties = pushProperties;
		this.objectMapper = objectMapper;
	}

	public boolean isConfigured() {
		return pushProperties.getWeb().isConfigured();
	}

	public String publicKey() {
		return pushProperties.getWeb().getVapidPublicKey();
	}

	/**
	 * @param subscriptionJson browser PushSubscription JSON
	 * @return true if sent successfully
	 */
	public boolean send(String subscriptionJson, String title, String body, Map<String, Object> data) {
		if (!isConfigured() || subscriptionJson == null || subscriptionJson.isBlank()) {
			return false;
		}
		try {
			Subscription sub = parseSubscription(subscriptionJson);
			Map<String, Object> payload = new HashMap<>();
			payload.put("title", title);
			payload.put("body", body);
			payload.put("data", data == null ? Map.of() : data);
			String json = objectMapper.writeValueAsString(payload);
			Notification notification = new Notification(sub, json);
			service().send(notification);
			return true;
		} catch (Exception e) {
			log.warn("webpush.send.failed: {}", e.getMessage());
			return false;
		}
	}

	private Subscription parseSubscription(String json) throws Exception {
		JsonNode root = objectMapper.readTree(json);
		String endpoint = root.path("endpoint").asText(null);
		JsonNode keys = root.path("keys");
		String p256dh = keys.path("p256dh").asText(null);
		String auth = keys.path("auth").asText(null);
		if (endpoint == null || p256dh == null || auth == null) {
			throw new IllegalArgumentException("Invalid push subscription JSON");
		}
		return new Subscription(endpoint, new Subscription.Keys(p256dh, auth));
	}

	private PushService service() throws Exception {
		PushService existing = pushService;
		if (existing != null) {
			return existing;
		}
		synchronized (this) {
			if (pushService == null) {
				PushService s = new PushService();
				s.setPublicKey(pushProperties.getWeb().getVapidPublicKey());
				s.setPrivateKey(pushProperties.getWeb().getVapidPrivateKey());
				s.setSubject(pushProperties.getWeb().getVapidSubject());
				pushService = s;
			}
			return pushService;
		}
	}
}
