package com.example.dating.model.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AiFeatureDailyUsagePk implements Serializable {
	@Column(name = "user_id", nullable = false)
	private Long userId;
	@Column(name = "usage_date", nullable = false)
	private LocalDate usageDate;
	@Column(name = "feature", nullable = false, length = 32)
	private String feature;

	public AiFeatureDailyUsagePk() {
	}

	public AiFeatureDailyUsagePk(Long userId, LocalDate usageDate, String feature) {
		this.userId = userId;
		this.usageDate = usageDate;
		this.feature = feature;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public LocalDate getUsageDate() {
		return usageDate;
	}

	public void setUsageDate(LocalDate usageDate) {
		this.usageDate = usageDate;
	}

	public String getFeature() {
		return feature;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AiFeatureDailyUsagePk that = (AiFeatureDailyUsagePk) o;
		return Objects.equals(userId, that.userId) && Objects.equals(usageDate, that.usageDate)
				&& Objects.equals(feature, that.feature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, usageDate, feature);
	}
}
