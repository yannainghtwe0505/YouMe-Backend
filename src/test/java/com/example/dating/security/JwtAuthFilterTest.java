package com.example.dating.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.example.dating.repository.PendingRegistrationRepo;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private PendingRegistrationRepo pendingRegistrationRepo;

	private JwtAuthFilter filter;

	@BeforeEach
	void setUp() {
		filter = new JwtAuthFilter(jwtService, userDetailsService, pendingRegistrationRepo);
	}

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void bearerJwt_setsAuthenticationFromUserDetailsService() throws Exception {
		when(jwtService.validateAndUserId("signed.token.here")).thenReturn(7L);
		UserDetails principal = new User("7", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
		when(userDetailsService.loadUserByUsername("7")).thenReturn(principal);

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRequestURI("/blocks/1");
		req.addHeader("Authorization", "Bearer signed.token.here");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilterInternal(req, res, chain);

		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals("7", SecurityContextHolder.getContext().getAuthentication().getName());
		verify(jwtService).validateAndUserId("signed.token.here");
		verify(userDetailsService).loadUserByUsername("7");
	}

	@Test
	void publicAuthLoginPath_skipsJwtParsing() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRequestURI("/auth/login");
		req.setMethod("POST");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(req, res, chain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
