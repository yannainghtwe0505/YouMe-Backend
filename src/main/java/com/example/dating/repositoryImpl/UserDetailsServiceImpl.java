package com.example.dating.repositoryImpl;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.dating.repository.UserRepo;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserRepo repo;

	public UserDetailsServiceImpl(UserRepo repo) {
		this.repo = repo;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Long id = Long.valueOf(username);
		var u = repo.findById(id).orElseThrow(() -> new UsernameNotFoundException("not found"));
		return new User(String.valueOf(u.getId()), u.getPasswordHash(), List.of(new SimpleGrantedAuthority("USER")));
	}
}
