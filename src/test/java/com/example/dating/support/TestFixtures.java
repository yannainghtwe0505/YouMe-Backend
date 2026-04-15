package com.example.dating.support;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Shared realistic-ish data and MockMvc {@link org.springframework.test.web.servlet.request.RequestPostProcessor}s.
 */
public final class TestFixtures {

	/** Minimal valid JPEG (SOI + EOI) for multipart upload tests. */
	public static final byte[] TINY_JPEG = new byte[] {
			(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9,
	};

	private TestFixtures() {
	}

	/**
	 * Controllers use {@code Long.valueOf(me.getUsername())} — principal username must be the numeric user id.
	 */
	public static User userPrincipal(long userId) {
		return new User(String.valueOf(userId), "n/a", Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
	}

	/**
	 * Sets {@code Authorization} so {@code JwtAuthFilter} runs in {@code @WebMvcTest} (see
	 * {@code AbstractWebMvcSliceTest} for matching {@code JwtService} stubs).
	 */
	public static RequestPostProcessor userId(long userId) {
		return request -> {
			request.addHeader("Authorization", "Bearer mock." + userId);
			return request;
		};
	}
}
