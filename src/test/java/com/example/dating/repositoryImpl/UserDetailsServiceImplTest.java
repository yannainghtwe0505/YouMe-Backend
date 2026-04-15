package com.example.dating.repositoryImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import com.example.dating.model.entity.UserEntity;
import com.example.dating.repository.UserRepo;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
@Import(UserDetailsServiceImpl.class)
class UserDetailsServiceImplTest {

	@Autowired
	private UserRepo users;

	@Autowired
	private UserDetailsServiceImpl userDetailsService;

	@Test
	void loadUserByUsername_returnsUserWithNumericName() {
		UserEntity u = new UserEntity();
		u.setEmail("uds@example.com");
		u.setPasswordHash("stored");
		users.save(u);

		var details = userDetailsService.loadUserByUsername(String.valueOf(u.getId()));
		assertEquals(String.valueOf(u.getId()), details.getUsername());
		assertEquals("stored", details.getPassword());
	}

	@Test
	void loadUserByUsername_throwsWhenMissing() {
		assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("999999999"));
	}
}
