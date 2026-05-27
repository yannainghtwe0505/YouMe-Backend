package com.example.dating.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.UserReportEntity;

public interface UserReportRepo extends JpaRepository<UserReportEntity, Long> {
	List<UserReportEntity> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
}