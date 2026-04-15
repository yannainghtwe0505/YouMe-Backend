package com.example.dating.error;

import com.example.dating.dto.AiQuotaView;
import com.example.dating.model.subscription.AiFeature;
import com.example.dating.model.subscription.SubscriptionPlan;

public class AiCoachQuotaExceededException extends RuntimeException {
	private final AiQuotaView quota;
	private final AiFeature feature;
	private final SubscriptionPlan currentPlan;
	private final SubscriptionPlan suggestedUpgrade;

	public AiCoachQuotaExceededException(AiQuotaView quota) {
		super("AI daily limit reached");
		this.quota = quota;
		this.feature = AiFeature.CHAT_REPLY;
		this.currentPlan = SubscriptionPlan.FREE;
		this.suggestedUpgrade = SubscriptionPlan.PLUS;
	}

	public AiCoachQuotaExceededException(AiQuotaView quota, AiFeature feature, SubscriptionPlan currentPlan,
			SubscriptionPlan suggestedUpgrade) {
		super("AI daily limit reached for " + feature.name());
		this.quota = quota;
		this.feature = feature;
		this.currentPlan = currentPlan;
		this.suggestedUpgrade = suggestedUpgrade;
	}

	public AiQuotaView getQuota() {
		return quota;
	}

	public AiFeature getFeature() {
		return feature;
	}

	public SubscriptionPlan getCurrentPlan() {
		return currentPlan;
	}

	public SubscriptionPlan getSuggestedUpgrade() {
		return suggestedUpgrade;
	}
}
