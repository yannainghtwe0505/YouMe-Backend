package com.example.dating.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sends registration OTP by email (SMTP) when configured, and by SMS via Twilio when configured.
 * Falls back to logging the code in dev/test or when {@code app.registration.log-otp} is true.
 */
@Service
public class RegistrationOtpDeliveryService {

	private static final Logger log = LoggerFactory.getLogger(RegistrationOtpDeliveryService.class);

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	private final ObjectProvider<JavaMailSender> javaMailSender;
	private final Environment environment;

	@Value("${app.registration.email.from:noreply@localhost}")
	private String emailFrom;

	@Value("${app.registration.log-otp:false}")
	private boolean logOtpProperty;

	@Value("${app.registration.twilio.account-sid:}")
	private String twilioAccountSid;

	@Value("${app.registration.twilio.auth-token:}")
	private String twilioAuthToken;

	@Value("${app.registration.twilio.from-number:}")
	private String twilioFromNumber;

	public RegistrationOtpDeliveryService(ObjectProvider<JavaMailSender> javaMailSender, Environment environment) {
		this.javaMailSender = javaMailSender;
		this.environment = environment;
	}

	private boolean mayLogPlainOtp() {
		return logOtpProperty || environment.acceptsProfiles(Profiles.of("dev", "test"));
	}

	public void sendEmailVerificationCode(String toEmail, String plainCode) {
		log.info("Registration email OTP requested: to={}", maskEmail(toEmail));
		JavaMailSender sender = javaMailSender.getIfAvailable();
		boolean smtpConfigured = sender != null && StringUtils.hasText(lookupMailHost());
		if (smtpConfigured) {
			try {
				SimpleMailMessage msg = new SimpleMailMessage();
				msg.setFrom(emailFrom);
				msg.setTo(toEmail);
				msg.setSubject("Your YouMe verification code");
				msg.setText("Your YouMe verification code is: " + plainCode + "\n\n"
						+ "It expires in 10 minutes. If you did not request this, ignore this email.");
				sender.send(msg);
				log.info("Registration email OTP sent successfully: to={}", maskEmail(toEmail));
				return;
			} catch (Exception e) {
				log.error("Registration email OTP send failed: to={}", maskEmail(toEmail), e);
			}
		} else {
			log.warn("Registration email OTP: SMTP not configured (set spring.mail.host). to={}", maskEmail(toEmail));
		}
		if (mayLogPlainOtp()) {
			log.warn("Registration EMAIL OTP (dev/test or log-otp) for {}: {}", toEmail, plainCode);
			return;
		}
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
				"Email could not be sent. Configure spring.mail.* (SMTP) or enable app.registration.log-otp for local use.");
	}

	public void sendSmsVerificationCode(String phoneE164, String plainCode) {
		log.info("Registration SMS OTP requested: phone={}", maskPhone(phoneE164));
		String smsBody = "Your YouMe verification code is: " + plainCode + ". It expires in 10 minutes.";
		if (twilioConfigured()) {
			try {
				sendViaTwilio(phoneE164, smsBody);
				log.info("Registration SMS OTP sent via Twilio: phone={}", maskPhone(phoneE164));
				return;
			} catch (Exception e) {
				log.error("Twilio SMS send failed: phone={}", maskPhone(phoneE164), e);
			}
		}
		if (mayLogPlainOtp()) {
			if (twilioConfigured()) {
				log.warn("Registration SMS OTP (Twilio failed; using log-otp fallback) phone={} code={}",
						maskPhone(phoneE164), plainCode);
			} else {
				log.warn(
						"Registration SMS OTP (Twilio off: use code below locally, or set app.registration.twilio.* for real SMS) phone={} code={}",
						maskPhone(phoneE164), plainCode);
			}
			return;
		}
		log.error(
				"SMS not sent: configure Twilio (app.registration.twilio.account-sid, auth-token, from-number) or set app.registration.log-otp=true. phone={}",
				maskPhone(phoneE164));
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
				"SMS could not be sent. Configure Twilio (app.registration.twilio.*) or enable app.registration.log-otp for local testing.");
	}

	private boolean twilioConfigured() {
		return StringUtils.hasText(twilioAccountSid) && StringUtils.hasText(twilioAuthToken)
				&& StringUtils.hasText(twilioFromNumber);
	}

	private void sendViaTwilio(String toE164, String bodyText) throws Exception {
		String sid = twilioAccountSid.trim();
		String token = twilioAuthToken.trim();
		String from = twilioFromNumber.trim();
		String basic = Base64.getEncoder()
				.encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));
		String form = "To=" + URLEncoder.encode(toE164, StandardCharsets.UTF_8) + "&From="
				+ URLEncoder.encode(from, StandardCharsets.UTF_8) + "&Body="
				+ URLEncoder.encode(bodyText, StandardCharsets.UTF_8);
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json"))
				.header("Authorization", "Basic " + basic)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(form))
				.timeout(Duration.ofSeconds(25))
				.build();
		HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() < 200 || res.statusCode() >= 300) {
			throw new IllegalStateException("Twilio HTTP " + res.statusCode() + ": " + res.body());
		}
	}

	private String lookupMailHost() {
		return environment.getProperty("spring.mail.host");
	}

	private static String maskEmail(String email) {
		if (email == null || !email.contains("@"))
			return "***";
		int at = email.indexOf('@');
		if (at <= 1)
			return "***@" + email.substring(at + 1);
		return email.charAt(0) + "***@" + email.substring(at + 1);
	}

	private static String maskPhone(String phone) {
		if (phone == null || phone.length() < 6)
			return "***";
		return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 2);
	}
}
