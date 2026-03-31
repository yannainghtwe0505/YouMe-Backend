package com.example.dating.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "passes", uniqueConstraints = @UniqueConstraint(columnNames = { "from_user", "to_user" }))
public class PassEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "from_user", nullable = false)
	private Long fromUser;
	@Column(name = "to_user", nullable = false)
	private Long toUser;
	private Instant createdAt;

	@PrePersist
	void pre() {
		if (createdAt == null)
			createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getFromUser() {
		return fromUser;
	}

	public void setFromUser(Long fromUser) {
		this.fromUser = fromUser;
	}

	public Long getToUser() {
		return toUser;
	}

	public void setToUser(Long toUser) {
		this.toUser = toUser;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
