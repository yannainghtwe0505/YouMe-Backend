package com.example.dating.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "matches", uniqueConstraints = @UniqueConstraint(columnNames = { "user_a", "user_b" }))
public class MatchEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_a", nullable = false)
	private Long userA;
	@Column(name = "user_b", nullable = false)
	private Long userB;
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

	public Long getUserA() {
		return userA;
	}

	public void setUserA(Long userA) {
		this.userA = userA;
	}

	public Long getUserB() {
		return userB;
	}

	public void setUserB(Long userB) {
		this.userB = userB;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
