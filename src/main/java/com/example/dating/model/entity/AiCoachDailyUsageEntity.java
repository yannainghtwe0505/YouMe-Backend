package com.example.dating.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_coach_daily_usage")
public class AiCoachDailyUsageEntity {
	@EmbeddedId
	private AiCoachDailyUsagePk id;
	@Column(name = "usage_count", nullable = false)
	private int usageCount;

	public AiCoachDailyUsagePk getId() {
		return id;
	}

	public void setId(AiCoachDailyUsagePk id) {
		this.id = id;
	}

	public int getUsageCount() {
		return usageCount;
	}

	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}
}
