package com.example.dating.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "messages")
public class MessageEntity {
	public static final String KIND_USER = "user";
	public static final String KIND_ASSISTANT = "assistant";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long matchId;
	/** Null for assistant/system messages; DB column must be nullable (see V5/V6 migration or scripts). */
	@Column(name = "sender_id", nullable = true)
	private Long senderId;
	@Column(length = 2000)
	private String body;
	@Column(name = "message_kind", length = 24, nullable = false)
	@ColumnDefault("'user'")
	private String messageKind = KIND_USER;
	private Instant createdAt;

	@PrePersist
	void pre() {
		if (createdAt == null)
			createdAt = Instant.now();
		if (messageKind == null || messageKind.isBlank())
			messageKind = KIND_USER;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMatchId() {
		return matchId;
	}

	public void setMatchId(Long matchId) {
		this.matchId = matchId;
	}

	public Long getSenderId() {
		return senderId;
	}

	public void setSenderId(Long senderId) {
		this.senderId = senderId;
	}

	public String getMessageKind() {
		return messageKind;
	}

	public void setMessageKind(String messageKind) {
		this.messageKind = messageKind == null ? KIND_USER : messageKind;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
