package com.example.dating.model.entity;

import java.io.Serializable;
import java.util.Objects;

public class MatchReadStateId implements Serializable {
	private Long userId;
	private Long matchId;

	public MatchReadStateId() {
	}

	public MatchReadStateId(Long userId, Long matchId) {
		this.userId = userId;
		this.matchId = matchId;
	}

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MatchReadStateId that = (MatchReadStateId) o;
		return Objects.equals(userId, that.userId) && Objects.equals(matchId, that.matchId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, matchId);
	}
}
