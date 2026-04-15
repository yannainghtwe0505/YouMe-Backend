package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.config.MediaUrls;
import com.example.dating.model.entity.PhotoEntity;
import com.example.dating.repository.PhotoRepo;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.service.PhotoService;
import com.example.dating.service.PresignService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = PhotoController.class)
class PhotoControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PresignService presignService;

	@MockBean
	private PhotoRepo photoRepo;

	@MockBean
	private ProfileRepo profileRepo;

	@MockBean
	private MediaUrls mediaUrls;

	@MockBean
	private PhotoService photoService;

	@Test
	void listMine_returnsPhotoRows() throws Exception {
		PhotoEntity ph = new PhotoEntity();
		ph.setId(10L);
		ph.setUserId(5L);
		ph.setS3Key("uploads/5/a.jpg");
		ph.setPrimaryPhoto(true);
		ph.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
		when(photoRepo.findByUserIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(ph));
		when(mediaUrls.urlForKey("uploads/5/a.jpg")).thenReturn("https://cdn.example/a.jpg");
		mockMvc.perform(get("/photos").with(userId(5L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(10))
				.andExpect(jsonPath("$[0].url").value("https://cdn.example/a.jpg"));
	}
}
