package com.example.dating.model.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Single source of truth for cross-platform billing state (Stripe, Apple, Google).
 * Kept in sync with {@link ProfileEntity#subscriptionPlan} for legacy readers.
 */
@Entity
@Table(name = "user_subscription")
public class UserSubscriptionEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** FREE | PLUS | GOLD — effective tier when lifecycle allows access. */
	@Column(name = "plan_tier", nullable = false, length = 12)
	private String planTier = "FREE";

	@Column(name = "billing_provider", length = 16)
	private String billingProvider = "NONE";

	@Column(name = "external_subscription_id", length = 255)
	private String externalSubscriptionId;

	/** Last receipt blob, purchase token, or audit payload (bounded; avoid multi-MB). */
	@Column(name = "receipt_data", columnDefinition = "TEXT")
	private String receiptData;

	@Column(name = "lifecycle_status", nullable = false, length = 20)
	private String lifecycleStatus = "NONE";

	@Column(name = "current_period_end")
	private Instant currentPeriodEnd;

	@Column(name = "cancel_at_period_end", nullable = false)
	private boolean cancelAtPeriodEnd = false;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant n = Instant.now();
		if (createdAt == null)
			createdAt = n;
		updatedAt = n;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getPlanTier() {
		return planTier;
	}

	public void setPlanTier(String planTier) {
		this.planTier = planTier == null || planTier.isBlank() ? "FREE" : planTier;
	}

	public String getBillingProvider() {
		return billingProvider;
	}

	public void setBillingProvider(String billingProvider) {
		this.billingProvider = billingProvider == null || billingProvider.isBlank() ? "NONE" : billingProvider;
	}

	public String getExternalSubscriptionId() {
		return externalSubscriptionId;
	}

	public void setExternalSubscriptionId(String externalSubscriptionId) {
		this.externalSubscriptionId = externalSubscriptionId;
	}

	public String getReceiptData() {
		return receiptData;
	}

	public void setReceiptData(String receiptData) {
		this.receiptData = receiptData;
	}

	public String getLifecycleStatus() {
		return lifecycleStatus;
	}

	public void setLifecycleStatus(String lifecycleStatus) {
		this.lifecycleStatus = lifecycleStatus == null || lifecycleStatus.isBlank() ? "NONE" : lifecycleStatus;
	}

	public Instant getCurrentPeriodEnd() {
		return currentPeriodEnd;
	}

	public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
		this.currentPeriodEnd = currentPeriodEnd;
	}

	public boolean isCancelAtPeriodEnd() {
		return cancelAtPeriodEnd;
	}

	public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
		this.cancelAtPeriodEnd = cancelAtPeriodEnd;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
