package com.example.dating.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.AiCoachDailyUsageEntity;
import com.example.dating.model.entity.AiCoachDailyUsagePk;

public interface AiCoachDailyUsageRepo extends JpaRepository<AiCoachDailyUsageEntity, AiCoachDailyUsagePk> {
}
