package com.example.dating.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhotoStoragePolicyTest {

	@Test
	void validKeyForUser() {
		assertTrue(PhotoStoragePolicy.isValidUploadKeyForUser(42L, "uploads/42/550e8400-e29b-41d4-a716-446655440000_photo.jpg"));
	}

	@Test
	void wrongUserOrTraversalRejected() {
		assertFalse(PhotoStoragePolicy.isValidUploadKeyForUser(42L, "uploads/99/550e8400-e29b-41d4-a716-446655440000_x.jpg"));
		assertFalse(PhotoStoragePolicy.isValidUploadKeyForUser(42L, "uploads/42/../99/x.jpg"));
		assertFalse(PhotoStoragePolicy.isValidUploadKeyForUser(42L, "uploads/42/x/y.jpg"));
	}
}
