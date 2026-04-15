package com.example.dating.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.UserRepo;
import com.example.dating.service.AccountRegistrationService;
import com.example.dating.support.AbstractWebMvcSliceTest;
import com.example.dating.support.TestFixtures;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserRepo users;

	@MockBean
	private PasswordEncoder encoder;

	@MockBean
	private AccountRegistrationService registrationService;

	@Test
	void login_returns401_onUnknownUser() throws Exception {
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ghost@example.com\",\"password\":\"secret12\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("invalid creds"));
	}

	@Test
	void login_returns200_withToken() throws Exception {
		UserEntity u = new UserEntity();
		u.setId(44L);
		u.setEmail("pat@example.com");
		u.setPasswordHash("hash");
		u.setRegistrationComplete(true);
		u.setOnboardingStep("DONE");
		when(users.findByEmail("pat@example.com")).thenReturn(Optional.of(u));
		when(encoder.matches("secret12", "hash")).thenReturn(true);
		when(jwtService.generate(44L, "pat@example.com")).thenReturn("jwt-token");
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"pat@example.com\",\"password\":\"secret12\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt-token"))
				.andExpect(jsonPath("$.userId").value(44));
	}

	@Test
	void register_returnsServicePayload() throws Exception {
		when(registrationService.registerWithProfile(anyString(), anyString(), anyString(), any(MultipartFile[].class)))
				.thenReturn(new AccountRegistrationService.RegisterWithProfileResult("tok", 99L));
		MockMultipartFile photo = new MockMultipartFile("photos", "a.jpg", "image/jpeg", TestFixtures.TINY_JPEG);
		mockMvc.perform(multipart("/auth/register")
				.file(photo)
				.param("email", "new@example.com")
				.param("password", "password1")
				.param("displayName", "New User"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("tok"))
				.andExpect(jsonPath("$.userId").value(99));
		verify(registrationService).registerWithProfile(eq("new@example.com"), eq("password1"), eq("New User"),
				any(MultipartFile[].class));
	}
}
