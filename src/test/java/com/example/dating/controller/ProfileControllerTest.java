package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.service.AiCoachService;
import com.example.dating.service.OnboardingRegistrationService;
import com.example.dating.service.ProfileAvatarService;
import com.example.dating.service.SubscriptionPlanService;
import com.example.dating.service.TieredAiUsageService;
import com.example.dating.service.UserSubscriptionService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = ProfileController.class)
class ProfileControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProfileRepo repo;

	@MockBean
	private UserRepo users;

	@MockBean
	private PhotoRepo photoRepo;

	@MockBean
	private MediaUrls mediaUrls;

	@MockBean
	private PasswordEncoder passwordEncoder;

	@MockBean
	private OnboardingRegistrationService onboarding;

	@MockBean
	private AiCoachService aiCoachService;

	@MockBean
	private TieredAiUsageService tieredAiUsageService;

	@MockBean
	private SubscriptionPlanService subscriptionPlanService;

	@MockBean
	private UserSubscriptionService userSubscriptionService;

	@MockBean
	private ProfileAvatarService profileAvatarService;

	@Test
	void changePassword_returns400_whenCurrentWrong() throws Exception {
		UserEntity u = new UserEntity();
		u.setId(3L);
		u.setPasswordHash("stored-hash");
		when(users.findById(3L)).thenReturn(Optional.of(u));
		when(passwordEncoder.matches("wrong-old", "stored-hash")).thenReturn(false);
		mockMvc.perform(put("/me/password").with(userId(3L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"wrong-old\",\"newPassword\":\"newpass12\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("current password is incorrect"));
	}

	@Test
	void changePassword_returns200_whenValid() throws Exception {
		UserEntity u = new UserEntity();
		u.setId(3L);
		u.setPasswordHash("stored-hash");
		when(users.findById(3L)).thenReturn(Optional.of(u));
		when(passwordEncoder.matches("old-pass-1", "stored-hash")).thenReturn(true);
		when(passwordEncoder.encode("new-pass-2")).thenReturn("new-hash");
		mockMvc.perform(put("/me/password").with(userId(3L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"old-pass-1\",\"newPassword\":\"new-pass-2\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true));
		verify(users).save(u);
	}
}
