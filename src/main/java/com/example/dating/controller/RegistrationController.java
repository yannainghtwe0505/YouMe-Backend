package com.example.dating.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.dating.dto.RegistrationProfilePatch;
import com.example.dating.service.OnboardingRegistrationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/auth/registration")
@Validated
public class RegistrationController {

	private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

	private final OnboardingRegistrationService onboarding;

	public RegistrationController(OnboardingRegistrationService onboarding) {
		this.onboarding = onboarding;
	}

	public static class EmailSendReq {
		@NotBlank
		public String email;
	}

	public static class EmailVerifyReq {
		@NotBlank
		public String email;
		@NotBlank
		public String code;
	}

	public static class PhoneSendReq {
		@NotBlank
		public String phone;
		/** Optional; saved on the account when sign-up completes (not used for SMS). */
		public String email;
	}

	public static class PhoneVerifyReq {
		@NotBlank
		public String phone;
		@NotBlank
		public String code;
	}

	public static class PasswordCreateReq {
		@NotBlank
		public String pendingSessionToken;
		@NotBlank
		public String password;
	}

	@PostMapping("/email/send")
	public ResponseEntity<?> emailSend(@Valid @RequestBody EmailSendReq req) {
		log.info("POST /auth/registration/email/send (email suffix ...@{})",
				req.email != null && req.email.contains("@") ? req.email.substring(req.email.indexOf('@')) : "?");
		onboarding.sendEmailCode(req.email);
		return ResponseEntity.ok(Map.of("ok", true, "message", "If an account can be created, a code was sent."));
	}

	@PostMapping("/email/verify")
	public ResponseEntity<?> emailVerify(@Valid @RequestBody EmailVerifyReq req) {
		return ResponseEntity.ok(onboarding.verifyEmailCode(req.email, req.code));
	}

	@PostMapping("/phone/send")
	public ResponseEntity<?> phoneSend(@Valid @RequestBody PhoneSendReq req) {
		log.info("POST /auth/registration/phone/send");
		onboarding.sendPhoneCode(req.phone, req.email);
		return ResponseEntity.ok(Map.of("ok", true, "message", "If an account can be created, a code was sent."));
	}

	@PostMapping("/phone/verify")
	public ResponseEntity<?> phoneVerify(@Valid @RequestBody PhoneVerifyReq req) {
		return ResponseEntity.ok(onboarding.verifyPhoneCode(req.phone, req.code));
	}

	@PostMapping("/password")
	public ResponseEntity<?> createPassword(@Valid @RequestBody PasswordCreateReq req) {
		return ResponseEntity.ok(onboarding.createAccountWithPassword(req.pendingSessionToken, req.password));
	}

	@GetMapping("/tokyo-wards")
	public ResponseEntity<?> tokyoWards() {
		return ResponseEntity.ok(Map.of("wards", onboarding.tokyoWards()));
	}

	@GetMapping("/status")
	public ResponseEntity<?> status(@AuthenticationPrincipal User me) {
		String sub = me.getUsername();
		if (sub.startsWith("pending:")) {
			return ResponseEntity.ok(onboarding.statusPending(Long.parseLong(sub.substring(8))));
		}
		return ResponseEntity.ok(onboarding.status(Long.valueOf(sub)));
	}

	@PutMapping("/profile")
	public ResponseEntity<?> profile(@AuthenticationPrincipal User me, @RequestBody RegistrationProfilePatch patch) {
		String sub = me.getUsername();
		if (sub.startsWith("pending:")) {
			return ResponseEntity.ok(onboarding.applyProfilePatchPending(Long.parseLong(sub.substring(8)), patch));
		}
		return ResponseEntity.ok(onboarding.applyProfilePatch(Long.valueOf(sub), patch));
	}

	@PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> complete(@AuthenticationPrincipal User me, @RequestParam("photos") MultipartFile[] photos) {
		String sub = me.getUsername();
		if (sub.startsWith("pending:")) {
			return ResponseEntity.ok(onboarding.completeRegistrationPending(Long.parseLong(sub.substring(8)), photos));
		}
		return ResponseEntity.ok(onboarding.completeRegistration(Long.valueOf(sub), photos));
	}
}
