package com.example.dating.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.AiFeatureDailyUsageEntity;
import com.example.dating.model.entity.AiFeatureDailyUsagePk;

public interface AiFeatureDailyUsageRepo extends JpaRepository<AiFeatureDailyUsageEntity, AiFeatureDailyUsagePk> {
}
