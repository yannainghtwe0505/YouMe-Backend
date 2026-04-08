package com.example.dating.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.dating.model.entity.PendingRegistrationEntity;
import com.example.dating.repository.PendingRegistrationRepo;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
	private final JwtService jwt;
	private final UserDetailsService uds;
	private final PendingRegistrationRepo pendingRepo;

	public JwtAuthFilter(JwtService jwt, UserDetailsService uds, PendingRegistrationRepo pendingRepo) {
		this.jwt = jwt;
		this.uds = uds;
		this.pendingRepo = pendingRepo;
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		String path = pathWithoutContext(request);
		// Only login/register are public; other /auth/* routes (e.g. password change) need JWT parsed here.
		// Web_socket handshake uses ?token= on /ws/* (validated in ChatHandshakeInterceptor).
		if (path.startsWith("/ws/"))
			return true;
		if (path.equals("/auth/login") || path.equals("/auth/register"))
			return true;
		if (path.equals("/auth/registration/tokyo-wards"))
			return true;
		if (path.startsWith("/auth/registration/email/") || path.startsWith("/auth/registration/phone/"))
			return true;
		return path.equals("/auth/registration/password");
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

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		String auth = req.getHeader("Authorization");
		if (auth != null && auth.startsWith("Bearer ")) {
			String token = auth.substring(7).trim();
			if (!token.isEmpty()) {
				if (token.indexOf('.') > 0) {
					try {
						Long userId = jwt.validateAndUserId(token);
						UserDetails details = uds.loadUserByUsername(String.valueOf(userId));
						var authentication = new UsernamePasswordAuthenticationToken(details, null,
								details.getAuthorities());
						SecurityContextHolder.getContext().setAuthentication(authentication);
					} catch (Exception ignored) {
						/* invalid JWT */
					}
				} else {
					Optional<PendingRegistrationEntity> pend = pendingRepo.findBySessionToken(token);
					if (pend.isPresent()) {
						PendingRegistrationEntity row = pend.get();
						if (row.getPasswordHash() != null && row.getSessionExpiresAt() != null
								&& row.getSessionExpiresAt().isAfter(Instant.now())) {
							UserDetails details = User.withUsername("pending:" + row.getId()).password("")
									.authorities("ROLE_PENDING_ONBOARDING").build();
							SecurityContextHolder.getContext()
									.setAuthentication(new UsernamePasswordAuthenticationToken(details, null,
											details.getAuthorities()));
						}
					}
				}
			}
		}
		chain.doFilter(req, res);
	}
}
