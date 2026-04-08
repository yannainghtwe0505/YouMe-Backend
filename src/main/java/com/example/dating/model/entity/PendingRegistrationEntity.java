package com.example.dating.model.entity;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "pending_registrations")
public class PendingRegistrationEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 255)
	private String email;

	@Column(name = "phone_e164", length = 32)
	private String phoneE164;

	@Column(nullable = false, length = 16)
	private String channel;

	@Column(name = "code_hash", nullable = false, length = 255)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private int attempts = 0;

	@Column(name = "session_token", length = 64, unique = true)
	private String sessionToken;

	@Column(name = "session_expires_at")
	private Instant sessionExpiresAt;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Column(name = "onboarding_step", length = 40)
	private String onboardingStep;

	@Column(name = "tos_accepted_at")
	private Instant tosAcceptedAt;

	@Column(name = "privacy_accepted_at")
	private Instant privacyAcceptedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "profile_draft", columnDefinition = "jsonb")
	private Map<String, Object> profileDraft;

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

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public void setCodeHash(String codeHash) {
		this.codeHash = codeHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	public Instant getSessionExpiresAt() {
		return sessionExpiresAt;
	}

	public void setSessionExpiresAt(Instant sessionExpiresAt) {
		this.sessionExpiresAt = sessionExpiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
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

	public Map<String, Object> getProfileDraft() {
		return profileDraft;
	}

	public void setProfileDraft(Map<String, Object> profileDraft) {
		this.profileDraft = profileDraft;
	}
}
