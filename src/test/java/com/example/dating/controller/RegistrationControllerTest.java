package com.example.dating.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.OnboardingRegistrationService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = RegistrationController.class)
class RegistrationControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OnboardingRegistrationService onboarding;

	@Test
	void emailSend_returnsOk() throws Exception {
		mockMvc.perform(post("/auth/registration/email/send").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"signup@example.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true));
		verify(onboarding).sendEmailCode("signup@example.com");
	}

	@Test
	void emailSend_returns400_whenEmailBlank() throws Exception {
		mockMvc.perform(post("/auth/registration/email/send").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void tokyoWards_returnsList() throws Exception {
		when(onboarding.tokyoWards()).thenReturn(List.of("Shinjuku-ku"));
		mockMvc.perform(get("/auth/registration/tokyo-wards"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wards[0]").value("Shinjuku-ku"));
	}
}
