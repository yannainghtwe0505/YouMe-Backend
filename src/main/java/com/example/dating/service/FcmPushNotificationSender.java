package com.example.dating.service;

import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.dating.config.PushProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FcmPushNotificationSender {
	private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationSender.class);
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

	private final PushProperties pushProperties;

	public FcmPushNotificationSender(PushProperties pushProperties) {
		this.pushProperties = pushProperties;
	}

	public boolean isConfigured() {
		return pushProperties.getFcm().isConfigured();
	}

	public boolean send(String deviceToken, String title, String body, Map<String, Object> data) {
		if (!isConfigured() || deviceToken == null || deviceToken.isBlank()) {
			return false;
		}
		if (deviceToken.startsWith("{")) {
			return false;
		}
		try {
			ensureFirebase();
			Message.Builder builder = Message.builder()
					.setToken(deviceToken.trim())
					.setNotification(Notification.builder()
							.setTitle(title)
							.setBody(body)
							.build());
			if (data != null) {
				for (Map.Entry<String, Object> e : data.entrySet()) {
					if (e.getKey() != null && e.getValue() != null) {
						builder.putData(e.getKey(), String.valueOf(e.getValue()));
					}
				}
			}
			String id = FirebaseMessaging.getInstance().send(builder.build());
			log.debug("fcm.sent messageId={}", id);
			return true;
		} catch (FirebaseMessagingException e) {
			log.warn("fcm.send.failed: {}", e.getMessagingErrorCode());
			return false;
		} catch (Exception e) {
			log.warn("fcm.send.failed: {}", e.getMessage());
			return false;
		}
	}

	private void ensureFirebase() throws Exception {
		if (INITIALIZED.get()) {
			return;
		}
		synchronized (FcmPushNotificationSender.class) {
			if (INITIALIZED.get()) {
				return;
			}
			String path = pushProperties.getFcm().getCredentialsPath();
			try (FileInputStream stream = new FileInputStream(path)) {
				FirebaseOptions options = FirebaseOptions.builder()
						.setCredentials(GoogleCredentials.fromStream(stream))
						.build();
				if (FirebaseApp.getApps().isEmpty()) {
					FirebaseApp.initializeApp(options);
				}
				INITIALIZED.set(true);
			}
		}
	}
}
