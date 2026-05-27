package com.example.dating.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dating.model.entity.UserDeviceTokenEntity;

public interface UserDeviceTokenRepo extends JpaRepository<UserDeviceTokenEntity, Long> {
	Optional<UserDeviceTokenEntity> findByToken(String token);

	List<UserDeviceTokenEntity> findByUserId(Long userId);

	List<UserDeviceTokenEntity> findByUserIdAndEnabledTrue(Long userId);
}
