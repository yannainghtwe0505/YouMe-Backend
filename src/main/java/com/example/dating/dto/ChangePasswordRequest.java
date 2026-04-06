package com.example.dating.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
	@NotBlank
	public String currentPassword;
	@NotBlank
	@Size(min = 6, message = "at least 6 characters")
	public String newPassword;
}
