package com.example.dating.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.databind.ObjectMapper;

class PendingOnboardingRouteFilterTest {

	private final PendingOnboardingRouteFilter filter = new PendingOnboardingRouteFilter(new ObjectMapper());

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void blocksNonRegistrationRoutes_forPendingPrincipal() throws Exception {
		UserDetails pending = User.withUsername("pending:12").password("").authorities("ROLE_PENDING_ONBOARDING").build();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(pending, null, pending.getAuthorities()));

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setMethod("GET");
		req.setRequestURI("/feed");
		MockHttpServletResponse res = new MockHttpServletResponse();

		filter.doFilterInternal(req, res, new MockFilterChain());

		assertEquals(403, res.getStatus());
		assertTrue(res.getContentAsString().contains("Finish sign-up"));
	}

	@Test
	void allowsGetMe_forPendingPrincipal() throws Exception {
		UserDetails pending = User.withUsername("pending:12").password("").authorities("ROLE_PENDING_ONBOARDING").build();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(pending, null, pending.getAuthorities()));

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setMethod("GET");
		req.setRequestURI("/me");
		MockHttpServletResponse res = new MockHttpServletResponse();

		filter.doFilterInternal(req, res, new MockFilterChain());

		assertEquals(200, res.getStatus());
	}

	@Test
	void passesThrough_whenNotPending() throws Exception {
		User normal = new User("3", "x", List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(normal, null, normal.getAuthorities()));

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setMethod("GET");
		req.setRequestURI("/feed");
		MockHttpServletResponse res = new MockHttpServletResponse();

		filter.doFilterInternal(req, res, new MockFilterChain());

		assertEquals(200, res.getStatus());
	}
}
