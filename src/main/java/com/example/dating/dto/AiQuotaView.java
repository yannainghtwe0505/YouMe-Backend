package com.example.dating.dto;

public record AiQuotaView(int usedToday, int dailyLimit, int remaining, boolean fairUseCap) {

	public static AiQuotaView limited(int used, int dailyLimit, int remaining) {
		return new AiQuotaView(used, dailyLimit, remaining, false);
	}

	public static AiQuotaView fairUse(int used, int cap, int remaining) {
		return new AiQuotaView(used, cap, remaining, true);
	}
}
