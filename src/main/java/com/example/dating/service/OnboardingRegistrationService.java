package com.example.dating.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.dating.dto.MeResponse;
import com.example.dating.dto.RegistrationProfilePatch;
import com.example.dating.model.entity.PendingRegistrationEntity;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.PendingRegistrationRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.security.JwtService;
import com.example.dating.util.TokyoMunicipalities;

@Service
public class OnboardingRegistrationService {

	private static final Logger log = LoggerFactory.getLogger(OnboardingRegistrationService.class);

	public static final String CHANNEL_EMAIL = "EMAIL";
	public static final String CHANNEL_PHONE = "PHONE";

	private static final int MAX_VERIFY_ATTEMPTS = 10;
	private static final int CODE_TTL_MINUTES = 10;
	private static final int SESSION_TTL_HOURS = 24;
	private static final int MIN_PASSWORD = 6;
	private static final int MIN_NICKNAME = 2;
	private static final int MAX_NICKNAME = 30;

	private final PendingRegistrationRepo pendingRepo;
	private final UserRepo users;
	private final ProfileRepo profiles;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwt;
	private final AccountRegistrationService accountRegistration;
	private final RegistrationOtpDeliveryService otpDelivery;
	private final String devOtp;

	private final SecureRandom random = new SecureRandom();

	public OnboardingRegistrationService(PendingRegistrationRepo pendingRepo, UserRepo users, ProfileRepo profiles,
			PasswordEncoder passwordEncoder, JwtService jwt, AccountRegistrationService accountRegistration,
			RegistrationOtpDeliveryService otpDelivery,
			@org.springframework.beans.factory.annotation.Value("${app.registration.dev-otp:}") String devOtp) {
		this.pendingRepo = pendingRepo;
		this.users = users;
		this.profiles = profiles;
		this.passwordEncoder = passwordEncoder;
		this.jwt = jwt;
		this.accountRegistration = accountRegistration;
		this.otpDelivery = otpDelivery;
		this.devOtp = devOtp == null ? "" : devOtp.trim();
	}

	public static String normalizeEmail(String raw) {
		if (raw == null)
			return null;
		String s = raw.trim().toLowerCase();
		return s.isEmpty() ? null : s;
	}

	/** JP mobile-style input to E.164 (+81...). */
	public static String normalizeJpPhone(String raw) {
		if (raw == null)
			return null;
		String s = raw.replaceAll("[\\s-]", "");
		if (s.isEmpty())
			return null;
		if (s.startsWith("+81"))
			return s;
		if (s.startsWith("0"))
			return "+81" + s.substring(1);
		if (s.startsWith("81") && s.length() >= 10)
			return "+" + s;
		return "+81" + s;
	}

	private String randomDigits(int n) {
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++)
			sb.append(this.random.nextInt(10));
		return sb.toString();
	}

	private boolean devBypass(String code) {
		return !devOtp.isEmpty() && devOtp.equals(code != null ? code.trim() : "");
	}

	@Transactional
	public void sendEmailCode(String rawEmail) {
		String email = normalizeEmail(rawEmail);
		if (email == null || !email.contains("@")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required");
		}
		if (users.findByEmail(email).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered");
		}
		String code = randomDigits(6);
		String hash = passwordEncoder.encode(code);
		Instant exp = Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES);
		PendingRegistrationEntity row = pendingRepo.findByEmail(email).orElseGet(PendingRegistrationEntity::new);
		row.setEmail(email);
		row.setPhoneE164(null);
		row.setChannel(CHANNEL_EMAIL);
		row.setCodeHash(hash);
		row.setExpiresAt(exp);
		row.setAttempts(0);
		row.setSessionToken(null);
		row.setSessionExpiresAt(null);
		clearPendingOnboardingFields(row);
		pendingRepo.save(row);
		otpDelivery.sendEmailVerificationCode(email, code);
	}

	@Transactional
	public Map<String, String> verifyEmailCode(String rawEmail, String rawCode) {
		String email = normalizeEmail(rawEmail);
		String code = rawCode == null ? "" : rawCode.trim();
		if (email == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
		}
		PendingRegistrationEntity row = pendingRepo.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No verification in progress"));
		return verifyChannel(row, code);
	}

	@Transactional
	public void sendPhoneCode(String rawPhone) {
		String phone = normalizeJpPhone(rawPhone);
		if (phone == null || phone.length() < 12) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid Japan phone number is required");
		}
		if (users.findByPhoneE164(phone).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This phone number is already registered");
		}
		String code = randomDigits(6);
		String hash = passwordEncoder.encode(code);
		Instant exp = Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES);
		PendingRegistrationEntity row = pendingRepo.findByPhoneE164(phone).orElseGet(PendingRegistrationEntity::new);
		row.setEmail(null);
		row.setPhoneE164(phone);
		row.setChannel(CHANNEL_PHONE);
		row.setCodeHash(hash);
		row.setExpiresAt(exp);
		row.setAttempts(0);
		row.setSessionToken(null);
		row.setSessionExpiresAt(null);
		clearPendingOnboardingFields(row);
		pendingRepo.save(row);
		otpDelivery.sendSmsVerificationCode(phone, code);
	}

	@Transactional
	public Map<String, String> verifyPhoneCode(String rawPhone, String rawCode) {
		String phone = normalizeJpPhone(rawPhone);
		String code = rawCode == null ? "" : rawCode.trim();
		if (phone == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
		}
		PendingRegistrationEntity row = pendingRepo.findByPhoneE164(phone)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No verification in progress"));
		return verifyChannel(row, code);
	}

	private Map<String, String> verifyChannel(PendingRegistrationEntity row, String code) {
		if (row.getExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code expired. Request a new one.");
		}
		if (row.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Request a new code.");
		}
		row.setAttempts(row.getAttempts() + 1);
		boolean ok = devBypass(code) || passwordEncoder.matches(code, row.getCodeHash());
		if (!ok) {
			pendingRepo.save(row);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
		}
		clearPendingOnboardingFields(row);
		String sessionToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
		sessionToken = sessionToken.substring(0, 64);
		row.setSessionToken(sessionToken);
		row.setSessionExpiresAt(Instant.now().plus(SESSION_TTL_HOURS, ChronoUnit.HOURS));
		pendingRepo.save(row);
		return Map.of("pendingSessionToken", sessionToken);
	}

	private static void clearPendingOnboardingFields(PendingRegistrationEntity row) {
		row.setPasswordHash(null);
		row.setOnboardingStep(null);
		row.setProfileDraft(null);
		row.setTosAcceptedAt(null);
		row.setPrivacyAcceptedAt(null);
	}

	/**
	 * Stores the password and keeps the session token; no {@code users} row until
	 * {@link #completeRegistrationPending}.
	 */
	@Transactional
	public Map<String, Object> createAccountWithPassword(String sessionToken, String rawPassword) {
		if (sessionToken == null || sessionToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session token is required");
		}
		String pw = rawPassword == null ? "" : rawPassword;
		if (pw.length() < MIN_PASSWORD) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Password must be at least " + MIN_PASSWORD + " characters");
		}
		PendingRegistrationEntity row = pendingRepo.findBySessionToken(sessionToken.trim())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired session"));
		if (row.getSessionExpiresAt() == null || row.getSessionExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session expired. Start again.");
		}
		if (row.getEmail() != null && users.findByEmail(row.getEmail()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
		}
		if (row.getPhoneE164() != null && users.findByPhoneE164(row.getPhoneE164()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
		}

		row.setPasswordHash(passwordEncoder.encode(pw));
		row.setOnboardingStep("GENDER");
		row.setProfileDraft(new HashMap<>());
		row.setSessionExpiresAt(Instant.now().plus(SESSION_TTL_HOURS, ChronoUnit.HOURS));
		pendingRepo.save(row);

		return Map.of("token", row.getSessionToken(), "registrationComplete", false, "onboardingStep",
				row.getOnboardingStep());
	}

	@Transactional
	public Map<String, Object> applyProfilePatchPending(Long pendingId, RegistrationProfilePatch patch) {
		PendingRegistrationEntity row = pendingRepo.findById(pendingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
		assertPendingOnboarding(row);
		if (row.getEmail() != null && users.findByEmail(row.getEmail()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
		}
		if (row.getPhoneE164() != null && users.findByPhoneE164(row.getPhoneE164()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
		}

		Map<String, Object> draft = row.getProfileDraft() != null
				? new HashMap<>(row.getProfileDraft())
				: new HashMap<>();

		if (patch.gender != null) {
			String g = patch.gender.trim().toUpperCase();
			if (!g.equals("MALE") && !g.equals("FEMALE")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gender must be MALE or FEMALE");
			}
			draft.put("gender", g);
		}
		if (patch.birthday != null && !patch.birthday.isBlank()) {
			LocalDate bd = LocalDate.parse(patch.birthday.trim());
			int age = Period.between(bd, LocalDate.now()).getYears();
			if (age < 18) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must be 18 or older");
			}
			draft.put("birthday", bd.toString());
		}
		if (patch.city != null && !patch.city.isBlank()) {
			String c = patch.city.trim();
			if (!TokyoMunicipalities.isAllowed(c)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid Tokyo ward or city");
			}
			draft.put("city", "Tokyo - " + c);
		}
		if (patch.displayName != null) {
			String n = patch.displayName.trim();
			if (n.length() < MIN_NICKNAME || n.length() > MAX_NICKNAME) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Nickname must be " + MIN_NICKNAME + "-" + MAX_NICKNAME + " characters");
			}
			draft.put("displayName", n);
		}
		if (patch.interests != null) {
			List<String> cleaned = new ArrayList<>();
			for (String s : patch.interests) {
				if (s != null && !s.isBlank())
					cleaned.add(s.trim());
			}
			draft.put("interests", cleaned.isEmpty() ? null : cleaned);
		}
		if (patch.referralSource != null && !patch.referralSource.isBlank()) {
			Map<String, Object> life = lifestyleMapFromDraft(draft);
			life.put("referralSource", patch.referralSource.trim());
			draft.put("lifestyle", life);
		}
		if (patch.lifestyle != null) {
			Map<String, Object> life = lifestyleMapFromDraft(draft);
			for (Map.Entry<String, Object> e : patch.lifestyle.entrySet()) {
				if (e.getKey() != null && e.getValue() != null)
					life.put(e.getKey(), e.getValue());
			}
			draft.put("lifestyle", life);
		}
		Instant now = Instant.now();
		if (Boolean.TRUE.equals(patch.acceptTos)) {
			row.setTosAcceptedAt(now);
		}
		if (Boolean.TRUE.equals(patch.acceptPrivacy)) {
			row.setPrivacyAcceptedAt(now);
		}
		if (patch.onboardingStep != null && !patch.onboardingStep.isBlank()) {
			row.setOnboardingStep(patch.onboardingStep.trim());
		}

		row.setProfileDraft(draft);
		pendingRepo.save(row);

		return Map.of("ok", true, "onboardingStep", row.getOnboardingStep() == null ? "" : row.getOnboardingStep());
	}

	private static Map<String, Object> lifestyleMapFromDraft(Map<String, Object> draft) {
		Object raw = draft.get("lifestyle");
		if (raw instanceof Map<?, ?> m) {
			Map<String, Object> life = new HashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				life.put(String.valueOf(e.getKey()), e.getValue());
			}
			return life;
		}
		return new HashMap<>();
	}

	private ProfileEntity profileFromDraft(Map<String, Object> draft) {
		ProfileEntity p = new ProfileEntity();
		if (draft == null || draft.isEmpty()) {
			return p;
		}
		Object g = draft.get("gender");
		if (g != null && !String.valueOf(g).isBlank()) {
			p.setGender(String.valueOf(g).trim());
		}
		Object bd = draft.get("birthday");
		if (bd != null && !String.valueOf(bd).isBlank()) {
			p.setBirthday(LocalDate.parse(String.valueOf(bd).trim()));
		}
		Object city = draft.get("city");
		if (city != null && !String.valueOf(city).isBlank()) {
			p.setCity(String.valueOf(city).trim());
		}
		Object dn = draft.get("displayName");
		if (dn != null && !String.valueOf(dn).isBlank()) {
			p.setDisplayName(String.valueOf(dn).trim());
		}
		Object interests = draft.get("interests");
		if (interests instanceof List<?> list) {
			List<String> cleaned = new ArrayList<>();
			for (Object o : list) {
				if (o != null && !String.valueOf(o).isBlank())
					cleaned.add(String.valueOf(o).trim());
			}
			p.setInterests(cleaned.isEmpty() ? null : cleaned);
		}
		Object life = draft.get("lifestyle");
		if (life instanceof Map<?, ?> m) {
			Map<String, Object> lm = new HashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				lm.put(String.valueOf(e.getKey()), e.getValue());
			}
			p.setLifestyle(lm.isEmpty() ? null : lm);
		}
		return p;
	}

	private void assertPendingOnboarding(PendingRegistrationEntity row) {
		if (row.getPasswordHash() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Complete the password step first");
		}
		if (row.getSessionExpiresAt() == null || row.getSessionExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session expired. Start again.");
		}
	}

	@Transactional
	public Map<String, Object> applyProfilePatch(Long userId, RegistrationProfilePatch patch) {
		UserEntity user = users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		if (user.isRegistrationComplete()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration already completed");
		}
		ProfileEntity profile = profiles.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

		if (patch.gender != null) {
			String g = patch.gender.trim().toUpperCase();
			if (!g.equals("MALE") && !g.equals("FEMALE")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gender must be MALE or FEMALE");
			}
			profile.setGender(g);
		}
		if (patch.birthday != null && !patch.birthday.isBlank()) {
			LocalDate bd = LocalDate.parse(patch.birthday.trim());
			int age = Period.between(bd, LocalDate.now()).getYears();
			if (age < 18) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must be 18 or older");
			}
			profile.setBirthday(bd);
		}
		if (patch.city != null && !patch.city.isBlank()) {
			String c = patch.city.trim();
			if (!TokyoMunicipalities.isAllowed(c)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid Tokyo ward or city");
			}
			profile.setCity("Tokyo - " + c);
		}
		if (patch.displayName != null) {
			String n = patch.displayName.trim();
			if (n.length() < MIN_NICKNAME || n.length() > MAX_NICKNAME) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Nickname must be " + MIN_NICKNAME + "-" + MAX_NICKNAME + " characters");
			}
			profile.setDisplayName(n);
		}
		if (patch.interests != null) {
			List<String> cleaned = new ArrayList<>();
			for (String s : patch.interests) {
				if (s != null && !s.isBlank())
					cleaned.add(s.trim());
			}
			profile.setInterests(cleaned.isEmpty() ? null : cleaned);
		}
		if (patch.referralSource != null && !patch.referralSource.isBlank()) {
			Map<String, Object> life = profile.getLifestyle() != null
					? new HashMap<>(profile.getLifestyle())
					: new HashMap<>();
			life.put("referralSource", patch.referralSource.trim());
			profile.setLifestyle(life);
		}
		if (patch.lifestyle != null) {
			Map<String, Object> life = profile.getLifestyle() != null
					? new HashMap<>(profile.getLifestyle())
					: new HashMap<>();
			for (Map.Entry<String, Object> e : patch.lifestyle.entrySet()) {
				if (e.getKey() != null && e.getValue() != null)
					life.put(e.getKey(), e.getValue());
			}
			profile.setLifestyle(life);
		}
		Instant now = Instant.now();
		if (Boolean.TRUE.equals(patch.acceptTos)) {
			user.setTosAcceptedAt(now);
		}
		if (Boolean.TRUE.equals(patch.acceptPrivacy)) {
			user.setPrivacyAcceptedAt(now);
		}
		if (patch.onboardingStep != null && !patch.onboardingStep.isBlank()) {
			user.setOnboardingStep(patch.onboardingStep.trim());
		}

		users.save(user);
		profiles.save(profile);

		return Map.of("ok", true, "onboardingStep", user.getOnboardingStep() == null ? "" : user.getOnboardingStep());
	}

	@Transactional
	public Map<String, Object> completeRegistration(Long userId, MultipartFile[] photos) {
		UserEntity user = users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		if (user.isRegistrationComplete()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already completed");
		}
		ProfileEntity profile = profiles.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
		validateReadyForCompletion(user, profile);
		if (!accountRegistration.isS3Available()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Photo storage is not available. Configure AWS S3 to finish sign-up.");
		}
		accountRegistration.savePhotosForUser(userId, photos, profile);
		user.setRegistrationComplete(true);
		user.setOnboardingStep("DONE");
		users.save(user);
		String claim = user.getEmail() != null ? user.getEmail()
				: (user.getPhoneE164() != null ? user.getPhoneE164() : "");
		String token = jwt.generate(user.getId(), claim);
		return Map.of("token", token, "userId", user.getId(), "registrationComplete", true);
	}

	@Transactional
	public Map<String, Object> completeRegistrationPending(Long pendingId, MultipartFile[] photos) {
		PendingRegistrationEntity row = pendingRepo.findById(pendingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
		assertPendingOnboarding(row);
		if (row.getEmail() != null && users.findByEmail(row.getEmail()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
		}
		if (row.getPhoneE164() != null && users.findByPhoneE164(row.getPhoneE164()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
		}

		UserEntity tempUser = new UserEntity();
		tempUser.setTosAcceptedAt(row.getTosAcceptedAt());
		tempUser.setPrivacyAcceptedAt(row.getPrivacyAcceptedAt());
		ProfileEntity p = profileFromDraft(row.getProfileDraft());
		validateReadyForCompletion(tempUser, p);

		if (!accountRegistration.isS3Available()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Photo storage is not available. Configure AWS S3 to finish sign-up.");
		}

		UserEntity u = new UserEntity();
		if (CHANNEL_EMAIL.equals(row.getChannel())) {
			u.setEmail(row.getEmail());
		} else {
			u.setEmail(null);
			u.setPhoneE164(row.getPhoneE164());
		}
		u.setPasswordHash(row.getPasswordHash());
		u.setRegistrationComplete(false);
		u.setOnboardingStep(row.getOnboardingStep());
		u.setTosAcceptedAt(row.getTosAcceptedAt());
		u.setPrivacyAcceptedAt(row.getPrivacyAcceptedAt());
		u = users.save(u);
		Long userId = u.getId();

		p.setUserId(userId);
		profiles.save(p);

		accountRegistration.savePhotosForUser(userId, photos, p);

		u.setRegistrationComplete(true);
		u.setOnboardingStep("DONE");
		users.save(u);

		pendingRepo.delete(row);

		String claim = u.getEmail() != null ? u.getEmail()
				: (u.getPhoneE164() != null ? u.getPhoneE164() : "");
		String token = jwt.generate(u.getId(), claim);
		return Map.of("token", token, "userId", u.getId(), "registrationComplete", true);
	}

	private void validateReadyForCompletion(UserEntity user, ProfileEntity profile) {
		if (profile.getGender() == null || profile.getGender().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gender is required");
		}
		if (profile.getBirthday() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Birthday is required");
		}
		if (Period.between(profile.getBirthday(), LocalDate.now()).getYears() < 18) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must be 18 or older");
		}
		if (profile.getCity() == null || profile.getCity().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is required");
		}
		String suffix = profile.getCity().contains(" - ") ? profile.getCity().substring(profile.getCity().indexOf(" - ") + 3)
				: profile.getCity();
		if (!TokyoMunicipalities.isAllowed(suffix.trim())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Tokyo location");
		}
		if (profile.getDisplayName() == null || profile.getDisplayName().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname is required");
		}
		if (user.getTosAcceptedAt() == null || user.getPrivacyAcceptedAt() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terms and privacy must be accepted");
		}
		List<String> interests = profile.getInterests();
		if (interests == null || interests.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one interest");
		}
		Map<String, Object> life = profile.getLifestyle();
		Integer heightCm = extractHeightCm(life);
		if (heightCm == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Height (cm) is required");
		}
		if (heightCm < 120 || heightCm > 230) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Height must be between 120 and 230 cm");
		}
		// education, occupation, incomeBand are optional for onboarding completion
		requireLifestyleText(life, "relationshipIntention", "Relationship intention is required");
		requireLifestyleText(life, "drinking", "Drinking habit is required");
		requireLifestyleText(life, "smoking", "Smoking habit is required");
		requireLifestyleText(life, "living", "Living situation is required");
	}

	private static Integer extractHeightCm(Map<String, Object> life) {
		if (life == null) {
			return null;
		}
		Object v = life.get("heightCm");
		if (v == null) {
			v = life.get("height_cm");
		}
		if (v == null) {
			return null;
		}
		if (v instanceof Number) {
			return ((Number) v).intValue();
		}
		if (v instanceof String s && !s.isBlank()) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}

	private static void requireLifestyleText(Map<String, Object> life, String key, String message) {
		if (isBlankLifestyleValue(life == null ? null : life.get(key))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
	}

	private static boolean isBlankLifestyleValue(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof CharSequence cs) {
			return cs.toString().isBlank();
		}
		if (v instanceof Number || v instanceof Boolean) {
			return false;
		}
		return String.valueOf(v).isBlank();
	}

	public Map<String, Object> status(Long userId) {
		UserEntity user = users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		ProfileEntity profile = profiles.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
		Map<String, Object> out = new HashMap<>();
		out.put("registrationComplete", user.isRegistrationComplete());
		out.put("onboardingStep", user.getOnboardingStep());
		out.put("gender", profile.getGender());
		out.put("birthday", profile.getBirthday() != null ? profile.getBirthday().toString() : null);
		out.put("city", profile.getCity());
		out.put("displayName", profile.getDisplayName());
		out.put("interests", profile.getInterests());
		out.put("lifestyle", profile.getLifestyle());
		out.put("tosAccepted", user.getTosAcceptedAt() != null);
		out.put("privacyAccepted", user.getPrivacyAcceptedAt() != null);
		return out;
	}

	public Map<String, Object> statusPending(Long pendingId) {
		PendingRegistrationEntity row = pendingRepo.findById(pendingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
		assertPendingOnboarding(row);
		ProfileEntity p = profileFromDraft(row.getProfileDraft());
		Map<String, Object> out = new HashMap<>();
		out.put("registrationComplete", false);
		out.put("onboardingStep", row.getOnboardingStep());
		out.put("gender", p.getGender());
		out.put("birthday", p.getBirthday() != null ? p.getBirthday().toString() : null);
		out.put("city", p.getCity());
		out.put("displayName", p.getDisplayName());
		out.put("interests", p.getInterests());
		out.put("lifestyle", p.getLifestyle());
		out.put("tosAccepted", row.getTosAcceptedAt() != null);
		out.put("privacyAccepted", row.getPrivacyAcceptedAt() != null);
		return out;
	}

	public MeResponse meForPendingPrincipal(long pendingId) {
		PendingRegistrationEntity row = pendingRepo.findById(pendingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		assertPendingOnboarding(row);
		return MeResponse.fromPending(row, row.getProfileDraft());
	}

	public List<String> tokyoWards() {
		return new ArrayList<>(TokyoMunicipalities.all());
	}
}
