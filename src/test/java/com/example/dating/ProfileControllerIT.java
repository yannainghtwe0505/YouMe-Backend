package com.example.dating;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerIT {
	@Autowired
	MockMvc mvc;
	@Autowired
	JwtService jwt;
	@Autowired
	UserRepo users;
	@Autowired
	ProfileRepo profiles;
	@Autowired
	PasswordEncoder passwordEncoder;

	private Long aliceId;

	@BeforeEach
	void seedAlice() {
		String email = "alice-" + System.nanoTime() + "@example.com";
		UserEntity u = new UserEntity();
		u.setEmail(email);
		u.setPasswordHash(passwordEncoder.encode("secret"));
		u.setRegistrationComplete(true);
		u.setOnboardingStep("DONE");
		u = users.save(u);
		aliceId = u.getId();
		ProfileEntity p = new ProfileEntity();
		p.setUserId(aliceId);
		profiles.save(p);
	}

	@Test
	void upsertAndFetchProfile() throws Exception {
		UserEntity u = users.findById(aliceId).orElseThrow();
		String token = jwt.generate(aliceId, u.getEmail());
		mvc.perform(put("/me/profile").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Alice\",\"bio\":\"Hi\"}")).andExpect(status().isOk());
		mvc.perform(get("/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
	}
}
