package com.example.dating.auth;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.UserRepo;
import com.example.dating.security.JwtService;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
	private final UserRepo users;
	private final PasswordEncoder encoder;
	private final JwtService jwt;

	public AuthController(UserRepo users, PasswordEncoder encoder, JwtService jwt) {
		this.users = users;
		this.encoder = encoder;
		this.jwt = jwt;
	}

	public static class RegisterReq {
		@Email
		public String email;
		@NotBlank
		public String password;
	}

	public static class LoginReq {
		@Email
		public String email;
		@NotBlank
		public String password;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterReq req) {
		
		if (users.findByEmail(req.email).isPresent()) {
			return ResponseEntity.badRequest().body(Map.of("error", "email in use"));
		}
		
		UserEntity u = new UserEntity();
		u.setEmail(req.email);
		u.setPasswordHash(encoder.encode(req.password));
		u = users.save(u);
		String token = jwt.generate(u.getId(), u.getEmail());
		return ResponseEntity.ok(Map.of("token", token, "userId", u.getId()));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginReq req) {
		var u = users.findByEmail(req.email).orElse(null);
		System.out.println("Password length: " + req.password.length());
		System.out.println("User from DB: " + u);
		if (u == null || !encoder.matches(req.password, u.getPasswordHash())) {
			return ResponseEntity.status(401).body(Map.of("error", "invalid creds"));
		}
		String token = jwt.generate(u.getId(), u.getEmail());
		return ResponseEntity.ok(Map.of("token", token, "userId", u.getId()));
	}
}
