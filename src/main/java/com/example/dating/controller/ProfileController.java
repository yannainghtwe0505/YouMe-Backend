package com.example.dating.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dating.dto.MeResponse;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;

@RestController
@RequestMapping("/me")
public class ProfileController {
	private final ProfileRepo repo;
	private final UserRepo users;

	public ProfileController(ProfileRepo repo, UserRepo users) {
		this.repo = repo;
		this.users = users;
	}

	@GetMapping
	public ResponseEntity<?> get(@AuthenticationPrincipal User me) {
		Long id = Long.valueOf(me.getUsername());
		if (!repo.existsById(id))
			return ResponseEntity.notFound().build();
		var user = users.findById(id).orElseThrow();
		var profile = repo.findById(id).orElseThrow();
		return ResponseEntity.ok(MeResponse.from(user, profile));
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
		return repo.findById(id).map(p -> {
			p.setPremium(true);
			repo.save(p);
			return ResponseEntity.ok(Map.of("isPremium", true));
		}).orElse(ResponseEntity.notFound().build());
	}

}
