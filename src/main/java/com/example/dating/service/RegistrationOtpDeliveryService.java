package com.example.dating.service;

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
 * Sends registration OTP by email (SMTP) when configured; otherwise can log the code for local dev.
 * SMS path logs the code until a provider (e.g. Twilio) is wired in.
 */
@Service
public class RegistrationOtpDeliveryService {

	private static final Logger log = LoggerFactory.getLogger(RegistrationOtpDeliveryService.class);

	private final ObjectProvider<JavaMailSender> javaMailSender;
	private final Environment environment;

	@Value("${app.registration.email.from:noreply@localhost}")
	private String emailFrom;

	@Value("${app.registration.log-otp:false}")
	private boolean logOtpProperty;

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
		// Hook Twilio/AWS SNS here when credentials exist; until then, log when allowed.
		if (mayLogPlainOtp()) {
			log.warn("Registration SMS OTP (simulated - configure SMS provider for production) {}: {}", phoneE164,
					plainCode);
			return;
		}
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
				"SMS is not configured. Use app.registration.log-otp=true or the dev profile for local testing, or add an SMS provider.");
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
