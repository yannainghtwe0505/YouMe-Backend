package com.example.dating.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.ProfileRepo;

@RestController
@RequestMapping("/me")
public class ProfileController {
	private final ProfileRepo repo;

	public ProfileController(ProfileRepo repo) {
		this.repo = repo;
	}

	@GetMapping
	public ResponseEntity<?> get(@AuthenticationPrincipal User me) {
		Long id = Long.valueOf(me.getUsername());
		return ResponseEntity.of(repo.findById(id));
	}

	@PutMapping("/profile")
	public ResponseEntity<?> upsert(@AuthenticationPrincipal User me, @RequestBody ProfileEntity body) {
		body.setUserId(Long.valueOf(me.getUsername()));
		return ResponseEntity.ok(repo.save(body));
	}
	@PostMapping("/profile")
	public ResponseEntity<?> create(
	        @AuthenticationPrincipal User me,
	        @RequestBody ProfileEntity body) {

	    // prevent overwriting existing profile
	    if (repo.existsById(Long.valueOf(me.getUsername()))) {
	        return ResponseEntity.badRequest().body("Profile already exists");
	    }

	    body.setUserId(Long.valueOf(me.getUsername()));
	    return ResponseEntity.ok(repo.save(body));
	}

	@PostMapping("/upgrade")
	public ResponseEntity<?> upgrade(@AuthenticationPrincipal User me) {
		Long id = Long.valueOf(me.getUsername());
		if (!repo.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().body("upgraded");
	}

}
