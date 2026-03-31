package com.example.dating.config;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.dating.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {
	private final JwtAuthFilter jwtAuthFilter;
	private final UserDetailsService uds;
	private final ObjectMapper objectMapper;

	public SecurityConfig(JwtAuthFilter f, UserDetailsService uds, ObjectMapper objectMapper) {
		this.jwtAuthFilter = f;
		this.uds = uds;
		this.objectMapper = objectMapper;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationProvider authProvider() {
		var p = new DaoAuthenticationProvider();
		p.setUserDetailsService(uds);
		p.setPasswordEncoder(passwordEncoder());
		return p;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
		return cfg.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(this::writeUnauthorizedJson)
				.accessDeniedHandler(this::writeAccessDeniedJson))
			.authorizeHttpRequests(reg -> reg
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/auth/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.anyRequest().authenticated());
		http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	private void writeUnauthorizedJson(HttpServletRequest req, HttpServletResponse res, Exception e)
			throws IOException {
		res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		res.setCharacterEncoding("UTF-8");
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(res.getOutputStream(), Map.of("error", "Unauthorized"));
	}

	private void writeAccessDeniedJson(HttpServletRequest req, HttpServletResponse res, Exception e)
			throws IOException {
		if (pathWithoutContext(req).startsWith("/auth/")) {
			res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			res.setCharacterEncoding("UTF-8");
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(res.getOutputStream(),
					Map.of("error", "Unable to process this request. Please try again."));
			return;
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
		res.setCharacterEncoding("UTF-8");
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(res.getOutputStream(), Map.of("error", "Forbidden"));
	}

	private static String pathWithoutContext(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String ctx = request.getContextPath();
		if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
			return uri.substring(ctx.length());
		}
		return uri;
	}
}
