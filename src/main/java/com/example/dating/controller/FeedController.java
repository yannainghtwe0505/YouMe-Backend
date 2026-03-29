package com.example.dating.controller;

import com.example.dating.model.entity.*;
import com.example.dating.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/feed")
public class FeedController {
	private final ProfileRepo profiles;

	public FeedController(ProfileRepo profiles) {
		this.profiles = profiles;
	}

	@GetMapping
	public ResponseEntity<List<ProfileEntity>> feed(@AuthenticationPrincipal User me) {
		Long myId = Long.valueOf(me.getUsername());
		var all = profiles.findAll().stream().filter(p -> !p.getUserId().equals(myId)).limit(50).toList();
		return ResponseEntity.ok(all);
	}
}
