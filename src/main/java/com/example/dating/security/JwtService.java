package com.example.dating.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
	@Value("${app.jwt.secret}")
	private String secret;
	@Value("${app.jwt.expirationMinutes}")
	private int expMinutes;
	private Key key;

	@PostConstruct
	void init() {
		key = Keys.hmacShaKeyFor(secret.getBytes());
	}

	public String generate(Long userId, String email) {
		Instant now = Instant.now();
		Map<String, Object> claims = new HashMap<>();
		claims.put("email", email);
		return Jwts.builder().setSubject(String.valueOf(userId)).claim("email", email).setIssuedAt(Date.from(now))
				.setExpiration(Date.from(now.plusSeconds(expMinutes * 60L))).signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	public Long validateAndUserId(String token) {
		 var jwt = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
		    return Long.valueOf(jwt.getBody().getSubject()); // get userId from sub
	}
}
