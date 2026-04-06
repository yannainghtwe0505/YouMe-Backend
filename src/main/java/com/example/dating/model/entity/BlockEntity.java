package com.example.dating.model.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "blocks")
public class BlockEntity {
	@EmbeddedId
	private BlockId id;
	@Column(name = "created_at")
	private Instant createdAt;

	@PrePersist
	void pre() {
		if (createdAt == null)
			createdAt = Instant.now();
	}

	public BlockId getId() {
		return id;
	}

	public void setId(BlockId id) {
		this.id = id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
