package com.example.dating.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.push")
public class PushProperties {
	private final WebPush web = new WebPush();
	private final Fcm fcm = new Fcm();

	public static final class WebPush {
		private boolean enabled;
		private String vapidPublicKey = "";
		private String vapidPrivateKey = "";
		private String vapidSubject = "mailto:support@youme.app";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getVapidPublicKey() {
			return vapidPublicKey;
		}

		public void setVapidPublicKey(String vapidPublicKey) {
			this.vapidPublicKey = vapidPublicKey;
		}

		public String getVapidPrivateKey() {
			return vapidPrivateKey;
		}

		public void setVapidPrivateKey(String vapidPrivateKey) {
			this.vapidPrivateKey = vapidPrivateKey;
		}

		public String getVapidSubject() {
			return vapidSubject;
		}

		public void setVapidSubject(String vapidSubject) {
			this.vapidSubject = vapidSubject;
		}

		public boolean isConfigured() {
			return enabled && vapidPublicKey != null && !vapidPublicKey.isBlank()
					&& vapidPrivateKey != null && !vapidPrivateKey.isBlank();
		}
	}

	public static final class Fcm {
		private boolean enabled;
		private String credentialsPath = "";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getCredentialsPath() {
			return credentialsPath;
		}

		public void setCredentialsPath(String credentialsPath) {
			this.credentialsPath = credentialsPath;
		}

		public boolean isConfigured() {
			return enabled && credentialsPath != null && !credentialsPath.isBlank();
		}
	}

	public WebPush getWeb() {
		return web;
	}

	public Fcm getFcm() {
		return fcm;
	}
}
