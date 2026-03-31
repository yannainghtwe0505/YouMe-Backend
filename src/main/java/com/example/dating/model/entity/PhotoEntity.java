package com.example.dating.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "photos")
public class PhotoEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id", nullable = false)
	private Long userId;
	@Column(name = "s3_key", nullable = false, length = 512)
	private String s3Key;
	@Column(name = "is_primary", nullable = false)
	private Boolean primaryPhoto = false;
	@Column(name = "created_at")
	private Instant createdAt;

	@PrePersist
	void pre() {
		if (createdAt == null)
			createdAt = Instant.now();
		if (primaryPhoto == null)
			primaryPhoto = false;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(String s3Key) {
		this.s3Key = s3Key;
	}

	public Boolean getPrimaryPhoto() {
		return primaryPhoto;
	}

	public void setPrimaryPhoto(Boolean primaryPhoto) {
		this.primaryPhoto = primaryPhoto;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
