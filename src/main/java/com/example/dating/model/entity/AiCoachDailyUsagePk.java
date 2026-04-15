package com.example.dating.model.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AiCoachDailyUsagePk implements Serializable {
	@Column(name = "user_id", nullable = false)
	private Long userId;
	@Column(name = "usage_date", nullable = false)
	private LocalDate usageDate;

	public AiCoachDailyUsagePk() {
	}

	public AiCoachDailyUsagePk(Long userId, LocalDate usageDate) {
		this.userId = userId;
		this.usageDate = usageDate;
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AiCoachDailyUsagePk that = (AiCoachDailyUsagePk) o;
		return Objects.equals(userId, that.userId) && Objects.equals(usageDate, that.usageDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, usageDate);
	}
}
