package com.example.dating.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.UserHelpEventEntity;

public interface UserHelpEventRepo extends JpaRepository<UserHelpEventEntity, Long> {
}
