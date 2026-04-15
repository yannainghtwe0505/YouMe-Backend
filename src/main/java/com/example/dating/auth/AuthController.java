package com.example.dating.auth;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.UserRepo;
import com.example.dating.security.JwtService;
import com.example.dating.service.AccountRegistrationService;
import com.example.dating.service.OnboardingRegistrationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
	private final UserRepo users;
	private final PasswordEncoder encoder;
	private final JwtService jwt;
	private final AccountRegistrationService registrationService;

	public AuthController(UserRepo users, PasswordEncoder encoder, JwtService jwt,
			AccountRegistrationService registrationService) {
		this.users = users;
		this.encoder = encoder;
		this.jwt = jwt;
		this.registrationService = registrationService;
	}

	public static class LoginReq {
		/** Sign in with email (either email or phone required). */
		public String email;
		/** E.164 or domestic JP format; normalized server-side. */
		public String phone;
		@NotBlank
		public String password;
	}

	/**
	 * Sign-up: email, password, display name, and at least one image. User + profile + photos are created
	 * together (no account without a profile).
	 */
	@PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> register(
			@RequestParam("email") String email,
			@RequestParam("password") String password,
			@RequestParam("displayName") String displayName,
			@RequestParam("photos") MultipartFile[] photos) {
		var out = registrationService.registerWithProfile(email, password, displayName, photos);
		return ResponseEntity.ok(Map.of("token", out.token(), "userId", out.userId()));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
		UserEntity u = null;
		if (req.email != null && !req.email.isBlank()) {
			String em = OnboardingRegistrationService.normalizeEmail(req.email);
			if (em != null)
				u = users.findByEmail(em).orElse(null);
		}
		if (u == null && req.phone != null && !req.phone.isBlank()) {
			String ph = OnboardingRegistrationService.normalizeJpPhone(req.phone.trim());
			if (ph != null)
				u = users.findByPhoneE164(ph).orElse(null);
		}
		if (u == null || !encoder.matches(req.password, u.getPasswordHash())) {
			return ResponseEntity.status(401).body(Map.of("error", "invalid creds"));
		}
		String claim = u.getEmail() != null ? u.getEmail()
				: (u.getPhoneE164() != null ? u.getPhoneE164() : "");
		String token = jwt.generate(u.getId(), claim);
		return ResponseEntity.ok(Map.of("token", token, "userId", u.getId(), "registrationComplete",
				u.isRegistrationComplete(), "onboardingStep", u.getOnboardingStep() == null ? "" : u.getOnboardingStep()));
	}
}
