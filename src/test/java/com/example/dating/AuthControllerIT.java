package com.example.dating;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.S3ObjectPutService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthControllerIT {

	private static final byte[] TINY_JPEG = new byte[] {
			(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9,
	};

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	S3ObjectPutService s3ObjectPutService;

	@BeforeEach
	void stubS3() {
		when(s3ObjectPutService.isAvailable()).thenReturn(true);
		doNothing().when(s3ObjectPutService).putObject(anyString(), any(byte[].class), anyString());
	}

	@Test
	void registerAndLogin() throws Exception {
		MockMultipartFile photo = new MockMultipartFile("photos", "a.jpg", "image/jpeg", TINY_JPEG);
		mvc.perform(multipart("/auth/register")
				.file(photo)
				.param("email", "test@example.com")
				.param("password", "password")
				.param("displayName", "Test User")).andExpect(status().isOk());
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"test@example.com\",\"password\":\"password\"}")).andExpect(status().isOk());
	}

	@Test
	void changePasswordWithBearerToken() throws Exception {
		String email = "cp-" + UUID.randomUUID() + "@example.com";
		MockMultipartFile photo = new MockMultipartFile("photos", "a.jpg", "image/jpeg", TINY_JPEG);
		mvc.perform(multipart("/auth/register")
				.file(photo)
				.param("email", email)
				.param("password", "firstPass1")
				.param("displayName", "CP User"))
				.andExpect(status().isOk());
		String loginBody = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"firstPass1\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode loginJson = objectMapper.readTree(loginBody);
		String token = loginJson.get("token").asText();

		mvc.perform(put("/me/password").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"firstPass1\",\"newPassword\":\"secondPass2\"}"))
				.andExpect(status().isOk());

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"secondPass2\"}"))
				.andExpect(status().isOk());
	}
}
