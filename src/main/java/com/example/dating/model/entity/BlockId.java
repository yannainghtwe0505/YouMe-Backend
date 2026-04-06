package com.example.dating.model.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class BlockId implements Serializable {
	@Column(name = "blocker_id", nullable = false)
	private Long blockerId;
	@Column(name = "blocked_id", nullable = false)
	private Long blockedId;

	public BlockId() {
	}

	public BlockId(Long blockerId, Long blockedId) {
		this.blockerId = blockerId;
		this.blockedId = blockedId;
	}

	public Long getBlockerId() {
		return blockerId;
	}

	public void setBlockerId(Long blockerId) {
		this.blockerId = blockerId;
	}

	public Long getBlockedId() {
		return blockedId;
	}

	public void setBlockedId(Long blockedId) {
		this.blockedId = blockedId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof BlockId that))
			return false;
		return Objects.equals(blockerId, that.blockerId) && Objects.equals(blockedId, that.blockedId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(blockerId, blockedId);
	}
}
