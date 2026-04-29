package com.example.dating.util;

/**
 * Validates S3 object keys for profile photos. Keys must be owned by the user
 * (prefix {@code uploads/{userId}/}) to prevent registering another user's objects.
 */
public final class PhotoStoragePolicy {

	private PhotoStoragePolicy() {
	}

	/** True if key looks like the app's upload layout for the given user (matches PresignService / Lambda). */
	public static boolean isValidUploadKeyForUser(Long userId, String s3Key) {
		if (userId == null || s3Key == null) {
			return false;
		}
		String prefix = "uploads/" + userId + "/";
		if (!s3Key.startsWith(prefix) || s3Key.length() > 512) {
			return false;
		}
		if (s3Key.contains("..")) {
			return false;
		}
		String rest = s3Key.substring(prefix.length());
		return !rest.isEmpty() && !rest.contains("/");
	}
}
