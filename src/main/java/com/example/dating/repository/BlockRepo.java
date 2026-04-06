package com.example.dating.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.dating.model.entity.BlockEntity;
import com.example.dating.model.entity.BlockId;

public interface BlockRepo extends JpaRepository<BlockEntity, BlockId> {
	boolean existsByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

	@Query("select b.id.blockedId from BlockEntity b where b.id.blockerId = :blockerId")
	List<Long> findBlockedIdsByBlocker(@Param("blockerId") Long blockerId);

	@Query("select b.id.blockerId from BlockEntity b where b.id.blockedId = :blockedId")
	List<Long> findBlockerIdsByBlocked(@Param("blockedId") Long blockedId);
}
