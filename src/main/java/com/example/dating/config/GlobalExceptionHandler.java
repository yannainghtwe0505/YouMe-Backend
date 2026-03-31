package com.example.dating.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
