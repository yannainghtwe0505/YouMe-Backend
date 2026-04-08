package com.example.dating.security;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * In-progress sign-up uses a synthetic principal; only registration APIs and {@code GET /me} are allowed
 * until the account is created.
 */
public class PendingOnboardingRouteFilter extends OncePerRequestFilter {

	private static final String PENDING_ROLE = "ROLE_PENDING_ONBOARDING";

	private final ObjectMapper objectMapper;

	public PendingOnboardingRouteFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return false;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
			@NonNull FilterChain chain) throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !isPendingOnboarding(auth)) {
			chain.doFilter(req, res);
			return;
		}
		String path = pathWithoutContext(req);
		String method = req.getMethod();
		boolean allowed = path.startsWith("/auth/registration/")
				|| ("GET".equalsIgnoreCase(method) && "/me".equals(path));
		if (allowed) {
			chain.doFilter(req, res);
			return;
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
		res.setCharacterEncoding("UTF-8");
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(res.getOutputStream(), Map.of("error", "Finish sign-up before using this."));
	}

	private static boolean isPendingOnboarding(Authentication auth) {
		for (GrantedAuthority a : auth.getAuthorities()) {
			if (PENDING_ROLE.equals(a.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	private static String pathWithoutContext(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String ctx = request.getContextPath();
		if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
			uri = uri.substring(ctx.length());
		}
		int q = uri.indexOf('?');
		if (q >= 0) {
			uri = uri.substring(0, q);
		}
		if (uri.length() > 1 && uri.endsWith("/")) {
			uri = uri.substring(0, uri.length() - 1);
		}
		return uri;
	}
}
