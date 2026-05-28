package com.example.dating.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.dating.model.entity.UserDeviceTokenEntity;
import com.example.dating.repository.UserDeviceTokenRepo;

@Service
public class PushNotificationService {
	private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

	public static final String KIND_NEW_MESSAGE = "NEW_MESSAGE";
	public static final String KIND_NEW_MATCH = "NEW_MATCH";

	private final UserDeviceTokenRepo userDeviceTokenRepo;
	private final WebPushNotificationSender webPush;
	private final FcmPushNotificationSender fcmPush;

	public PushNotificationService(UserDeviceTokenRepo userDeviceTokenRepo, WebPushNotificationSender webPush,
			FcmPushNotificationSender fcmPush) {
		this.userDeviceTokenRepo = userDeviceTokenRepo;
		this.webPush = webPush;
		this.fcmPush = fcmPush;
	}

	public void notifyNewMessage(Long recipientUserId, Long matchId, String senderLabel, String preview) {
		if (recipientUserId == null || matchId == null) {
			return;
		}
		String title = senderLabel != null && !senderLabel.isBlank() ? senderLabel.trim() : "New message";
		String body = truncate(preview, 120);
		if (body.isBlank()) {
			body = "You have a new message";
		}
		dispatch(recipientUserId, KIND_NEW_MESSAGE, title, body, Map.of(
				"matchId", String.valueOf(matchId),
				"deepLink", "/messages/" + matchId));
	}

	public void notifyNewMatch(Long recipientUserId, Long matchId, String peerLabel) {
		if (recipientUserId == null || matchId == null) {
			return;
		}
		String title = "It's a match!";
		String body = peerLabel != null && !peerLabel.isBlank()
				? "You and " + peerLabel.trim() + " liked each other"
				: "You have a new match";
		dispatch(recipientUserId, KIND_NEW_MATCH, title, body, Map.of(
				"matchId", String.valueOf(matchId),
				"deepLink", "/messages/" + matchId));
	}

	private void dispatch(Long userId, String kind, String title, String body, Map<String, Object> data) {
		List<UserDeviceTokenEntity> tokens = userDeviceTokenRepo.findByUserIdAndEnabledTrue(userId);
		if (tokens.isEmpty()) {
			return;
		}
		for (UserDeviceTokenEntity row : tokens) {
			boolean sent = sendToToken(row, title, body, data);
			if (sent) {
				log.info("push.sent userId={} kind={} platform={}", userId, kind, row.getPlatform());
			} else {
				log.info("push.dispatch userId={} kind={} platform={} title={} (not delivered — check VAPID/FCM config)",
						userId, kind, row.getPlatform(), title);
			}
		}
	}

	private boolean sendToToken(UserDeviceTokenEntity row, String title, String body, Map<String, Object> data) {
		String token = row.getToken();
		if (token == null || token.isBlank()) {
			return false;
		}
		String trimmed = token.trim();
		if (trimmed.startsWith("{")) {
			if (webPush.isConfigured()) {
				return webPush.send(trimmed, title, body, data);
			}
			return false;
		}
		if (fcmPush.isConfigured()) {
			return fcmPush.send(trimmed, title, body, data);
		}
		return false;
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		String t = s.strip();
		if (t.length() <= max) {
			return t;
		}
		return t.substring(0, max - 1) + "…";
	}
}
