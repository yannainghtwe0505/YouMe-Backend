package com.example.dating.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.ProfileEntity;

public interface ProfileRepo extends JpaRepository<ProfileEntity, Long> {
}
