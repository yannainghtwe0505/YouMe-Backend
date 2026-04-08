package com.example.dating.chat;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.example.dating.security.JwtService;

@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
	private final JwtService jwtService;

	public ChatHandshakeInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		if (!(request instanceof ServletServerHttpRequest servletRequest))
			return false;
		String token = servletRequest.getServletRequest().getParameter("token");
		if (token == null || token.isBlank())
			return false;
		try {
			Long userId = jwtService.validateAndUserId(token);
			attributes.put("userId", userId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception ex) {
	}
}
