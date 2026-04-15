package com.example.dating.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "users")
public class UserEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true, length = 255)
	private String email;
	@Column(name = "phone_e164", unique = true, length = 32)
	private String phoneE164;
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "last_login")
	private Instant lastLogin;
	@Column(name = "registration_complete", nullable = false)
	@ColumnDefault("true")
	private boolean registrationComplete = true;
	@Column(name = "onboarding_step", length = 40)
	private String onboardingStep;
	@Column(name = "tos_accepted_at")
	private Instant tosAcceptedAt;
	@Column(name = "privacy_accepted_at")
	private Instant privacyAcceptedAt;
	@Column(length = 12)
	private String locale;

	@PrePersist
	void pre() {
		if (createdAt == null)
			createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneE164() {
		return phoneE164;
	}

	public void setPhoneE164(String phoneE164) {
		this.phoneE164 = phoneE164;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String p) {
		this.passwordHash = p;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant i) {
		this.createdAt = i;
	}

	public Instant getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Instant i) {
		this.lastLogin = i;
	}

	public boolean isRegistrationComplete() {
		return registrationComplete;
	}

	public void setRegistrationComplete(boolean registrationComplete) {
		this.registrationComplete = registrationComplete;
	}

	public String getOnboardingStep() {
		return onboardingStep;
	}

	public void setOnboardingStep(String onboardingStep) {
		this.onboardingStep = onboardingStep;
	}

	public Instant getTosAcceptedAt() {
		return tosAcceptedAt;
	}

	public void setTosAcceptedAt(Instant tosAcceptedAt) {
		this.tosAcceptedAt = tosAcceptedAt;
	}

	public Instant getPrivacyAcceptedAt() {
		return privacyAcceptedAt;
	}

	public void setPrivacyAcceptedAt(Instant privacyAcceptedAt) {
		this.privacyAcceptedAt = privacyAcceptedAt;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}
}
