package com.example.dating;
import com.example.dating.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ProfileControllerIT {
  @Autowired MockMvc mvc; @Autowired JwtService jwt;
  @Test void upsertAndFetchProfile() throws Exception {
    String token = jwt.generate(1L, "alice@example.com");
    mvc.perform(put("/me/profile").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
      .content("{\"displayName\":\"Alice\",\"bio\":\"Hi\"}")).andExpect(status().isOk());
    mvc.perform(get("/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  }
}
