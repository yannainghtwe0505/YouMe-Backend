package com.example.dating.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.dto.AiQuotaView;
import com.example.dating.error.AiCoachQuotaExceededException;
import com.example.dating.model.subscription.SubscriptionPlan;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static boolean isAuthPath(HttpServletRequest req) {
		String uri = req.getRequestURI();
		String ctx = req.getContextPath();
		if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
			uri = uri.substring(ctx.length());
		}
		return uri.startsWith("/auth/");
	}

	@ExceptionHandler(AiCoachQuotaExceededException.class)
	public ResponseEntity<Map<String, Object>> handleAiCoachQuota(AiCoachQuotaExceededException ex) {
		AiQuotaView q = ex.getQuota();
		Map<String, Object> quota = new HashMap<>();
		quota.put("usedToday", q.usedToday());
		quota.put("dailyLimit", q.dailyLimit());
		quota.put("remaining", q.remaining());
		quota.put("fairUseCap", q.fairUseCap());
		Map<String, Object> body = new HashMap<>();
		body.put("error", "Daily AI limit reached for this feature. Upgrade for more.");
		body.put("aiQuota", quota);
		body.put("feature", ex.getFeature().apiKey());
		body.put("currentPlan", ex.getCurrentPlan().name());
		SubscriptionPlan up = ex.getSuggestedUpgrade();
		if (up != null)
			body.put("suggestedUpgrade", up.name());
		String hint = upgradeHint(ex.getCurrentPlan(), up);
		if (hint == null && q.fairUseCap() && q.remaining() <= 0)
			hint = "Daily fair-use cap reached. Resets on the next calendar day (see server timezone).";
		body.put("upgradeHint", hint);
		return ResponseEntity.status(429).body(body);
	}

	private static String upgradeHint(SubscriptionPlan current, SubscriptionPlan suggested) {
		if (suggested == null)
			return null;
		if (current == SubscriptionPlan.FREE && suggested == SubscriptionPlan.PLUS)
			return "YouMe Plus adds higher daily AI limits, smarter reply ideas, and match insights.";
		if (current == SubscriptionPlan.PLUS && suggested == SubscriptionPlan.GOLD)
			return "YouMe Gold unlocks deep compatibility reports, strategist-level chat help, and maximum AI depth.";
		return "Upgrade to unlock more YouMe AI.";
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
		int code = ex.getStatusCode().value();
		String reason = ex.getReason() != null ? ex.getReason() : "Request failed";
		return ResponseEntity.status(code).body(Map.of("error", reason));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex, HttpServletRequest req) {
		log.error("Data access error on {}", req.getRequestURI(), ex);
		if (isAuthPath(req)) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "Unable to complete sign-in or registration. Please try again later."));
		}
		return ResponseEntity.internalServerError()
				.body(Map.of("error", "Something went wrong. Please try again later."));
	}
}
