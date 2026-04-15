package com.example.dating.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionProperties {

	private boolean stripeEnabled = false;
	private String secretKey = "";
	private String webhookSecret = "";
	private String successPath = "/upgrade/success";
	private String cancelPath = "/upgrade";
	private String currency = "jpy";
	private int plusPriceMinor = 980;
	private int goldPriceMinor = 1980;
	private String stripePricePlusMonthly = "";
	private String stripePriceGoldMonthly = "";

	/** App Store shared secret for verifyReceipt (subscriptions). */
	private String appleSharedSecret = "";
	private String appleProductPlusMonthly = "";
	private String appleProductGoldMonthly = "";

	/** Android applicationId (package name) and service-account JSON path for Play Developer API. */
	private String googlePlayPackageName = "";
	private String googlePlayCredentialsJson = "";
	private String googleProductPlusMonthly = "";
	private String googleProductGoldMonthly = "";

	public boolean isStripeEnabled() {
		return stripeEnabled;
	}

	public void setStripeEnabled(boolean stripeEnabled) {
		this.stripeEnabled = stripeEnabled;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getWebhookSecret() {
		return webhookSecret;
	}

	public void setWebhookSecret(String webhookSecret) {
		this.webhookSecret = webhookSecret;
	}

	public String getSuccessPath() {
		return successPath;
	}

	public void setSuccessPath(String successPath) {
		this.successPath = successPath;
	}

	public String getCancelPath() {
		return cancelPath;
	}

	public void setCancelPath(String cancelPath) {
		this.cancelPath = cancelPath;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public int getPlusPriceMinor() {
		return plusPriceMinor;
	}

	public void setPlusPriceMinor(int plusPriceMinor) {
		this.plusPriceMinor = plusPriceMinor;
	}

	public int getGoldPriceMinor() {
		return goldPriceMinor;
	}

	public void setGoldPriceMinor(int goldPriceMinor) {
		this.goldPriceMinor = goldPriceMinor;
	}

	public String getStripePricePlusMonthly() {
		return stripePricePlusMonthly;
	}

	public void setStripePricePlusMonthly(String stripePricePlusMonthly) {
		this.stripePricePlusMonthly = stripePricePlusMonthly;
	}

	public String getStripePriceGoldMonthly() {
		return stripePriceGoldMonthly;
	}

	public void setStripePriceGoldMonthly(String stripePriceGoldMonthly) {
		this.stripePriceGoldMonthly = stripePriceGoldMonthly;
	}

	public boolean stripeReady() {
		return stripeEnabled && secretKey != null && !secretKey.isBlank()
				&& stripePricePlusMonthly != null && !stripePricePlusMonthly.isBlank()
				&& stripePriceGoldMonthly != null && !stripePriceGoldMonthly.isBlank();
	}

	public String getAppleSharedSecret() {
		return appleSharedSecret;
	}

	public void setAppleSharedSecret(String appleSharedSecret) {
		this.appleSharedSecret = appleSharedSecret;
	}

	public String getAppleProductPlusMonthly() {
		return appleProductPlusMonthly;
	}

	public void setAppleProductPlusMonthly(String appleProductPlusMonthly) {
		this.appleProductPlusMonthly = appleProductPlusMonthly;
	}

	public String getAppleProductGoldMonthly() {
		return appleProductGoldMonthly;
	}

	public void setAppleProductGoldMonthly(String appleProductGoldMonthly) {
		this.appleProductGoldMonthly = appleProductGoldMonthly;
	}

	public String getGooglePlayPackageName() {
		return googlePlayPackageName;
	}

	public void setGooglePlayPackageName(String googlePlayPackageName) {
		this.googlePlayPackageName = googlePlayPackageName;
	}

	public String getGooglePlayCredentialsJson() {
		return googlePlayCredentialsJson;
	}

	public void setGooglePlayCredentialsJson(String googlePlayCredentialsJson) {
		this.googlePlayCredentialsJson = googlePlayCredentialsJson;
	}

	public String getGoogleProductPlusMonthly() {
		return googleProductPlusMonthly;
	}

	public void setGoogleProductPlusMonthly(String googleProductPlusMonthly) {
		this.googleProductPlusMonthly = googleProductPlusMonthly;
	}

	public String getGoogleProductGoldMonthly() {
		return googleProductGoldMonthly;
	}

	public void setGoogleProductGoldMonthly(String googleProductGoldMonthly) {
		this.googleProductGoldMonthly = googleProductGoldMonthly;
	}

	public boolean appleIapConfigured() {
		return appleSharedSecret != null && !appleSharedSecret.isBlank()
				&& appleProductPlusMonthly != null && !appleProductPlusMonthly.isBlank()
				&& appleProductGoldMonthly != null && !appleProductGoldMonthly.isBlank();
	}

	public boolean googlePlayConfigured() {
		return googlePlayPackageName != null && !googlePlayPackageName.isBlank()
				&& googlePlayCredentialsJson != null && !googlePlayCredentialsJson.isBlank()
				&& googleProductPlusMonthly != null && !googleProductPlusMonthly.isBlank()
				&& googleProductGoldMonthly != null && !googleProductGoldMonthly.isBlank();
	}
}
