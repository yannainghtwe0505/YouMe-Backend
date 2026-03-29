package com.example.dating.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.service.PresignService;

@RestController
@RequestMapping("/photos")
public class PhotoController {
	private final PresignService presign;

	public PhotoController(PresignService presign) {
		this.presign = presign;
	}

	@PostMapping("/presign")
	public ResponseEntity<?> presign(@AuthenticationPrincipal User me, @RequestParam String filename,
			@RequestParam String contentType) {
		var url = presign.presignUpload(filename, contentType);
		return ResponseEntity.ok(Map.of("uploadUrl", url.toString()));
	}

	@PostMapping("/complete")
	public ResponseEntity<?> complete(@AuthenticationPrincipal User me, @RequestParam String s3Key) {
		return ResponseEntity.created(URI.create("s3://" + s3Key)).build();
	}
}
