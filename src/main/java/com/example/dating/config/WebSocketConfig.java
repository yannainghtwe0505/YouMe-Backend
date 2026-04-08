package com.example.dating.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.dating.chat.ChatHandshakeInterceptor;
import com.example.dating.chat.MatchChatWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
	private final MatchChatWebSocketHandler chatHandler;
	private final ChatHandshakeInterceptor handshakeInterceptor;

	public WebSocketConfig(MatchChatWebSocketHandler chatHandler, ChatHandshakeInterceptor handshakeInterceptor) {
		this.chatHandler = chatHandler;
		this.handshakeInterceptor = handshakeInterceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(chatHandler, "/ws/chat")
				.addInterceptors(handshakeInterceptor)
				.setAllowedOriginPatterns("*");
	}
}
