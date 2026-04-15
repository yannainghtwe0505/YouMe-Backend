package com.example.dating.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.example.dating.config.SecurityConfig;
import com.example.dating.repository.PendingRegistrationRepo;
import com.example.dating.security.JwtAuthFilter;
import com.example.dating.security.JwtService;

/**
 * Mocks required by {@code JwtAuthFilter} and {@code SecurityConfig} when a
 * {@code @WebMvcTest} slice loads the main application configuration.
 */
@Import({ SecurityConfig.class, JwtAuthFilter.class })
public abstract class AbstractWebMvcSliceTest {

	@MockBean
	protected JwtService jwtService;

	@MockBean
	protected UserDetailsService userDetailsService;

	@MockBean
	protected PendingRegistrationRepo pendingRegistrationRepo;

	@BeforeEach
	void wireSliceJwtAuth() {
		lenient().when(jwtService.validateAndUserId(argThat(t -> t != null && t.startsWith("mock."))))
				.thenAnswer(inv -> Long.parseLong(((String) inv.getArgument(0)).substring(5)));
		lenient().when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(inv -> {
			String name = inv.getArgument(0, String.class);
			if (name != null && name.startsWith("pending:")) {
				return User.withUsername(name).password("").authorities("ROLE_PENDING_ONBOARDING").build();
			}
			return TestFixtures.userPrincipal(Long.parseLong(name));
		});
	}
}
