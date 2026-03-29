package com.example.dating.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.UserEntity;

import java.util.Optional;

public interface UserRepo extends JpaRepository<UserEntity, Long> {
	Optional<UserEntity> findByEmail(String email);
}
