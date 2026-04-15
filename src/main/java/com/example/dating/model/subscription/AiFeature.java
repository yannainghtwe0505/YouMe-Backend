package com.example.dating.model.subscription;

import java.util.Locale;

/** Metered AI surfaces (usage tracked per plan per day). */
public enum AiFeature {
	CHAT_REPLY,
	PROFILE_AI,
	MATCH_GREETING,
	MATCH_INSIGHT;

	public String apiKey() {
		return name().toLowerCase(Locale.ROOT).replace('_', '-');
	}
}
