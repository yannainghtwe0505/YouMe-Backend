package com.example.dating.model.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "match_read_state")
@IdClass(MatchReadStateId.class)
public class MatchReadState {
	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Id
	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Column(name = "last_read_at", nullable = false)
	private Instant lastReadAt;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getMatchId() {
		return matchId;
	}

	public void setMatchId(Long matchId) {
		this.matchId = matchId;
	}

	public Instant getLastReadAt() {
		return lastReadAt;
	}

	public void setLastReadAt(Instant lastReadAt) {
		this.lastReadAt = lastReadAt;
	}
}
