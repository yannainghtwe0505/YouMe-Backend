package com.example.dating.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChangePasswordRequestTest {

	@Test
	void holdsPasswordFields() {
		ChangePasswordRequest r = new ChangePasswordRequest();
		r.currentPassword = "old-one";
		r.newPassword = "new-one";
		assertEquals("old-one", r.currentPassword);
		assertEquals("new-one", r.newPassword);
	}
}
