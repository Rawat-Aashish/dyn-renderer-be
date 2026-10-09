package com.dynamicformbuilder.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.dynamicformbuilder.config.AppProperties;
import com.dynamicformbuilder.security.Role;
import com.dynamicformbuilder.user.User;
import com.dynamicformbuilder.user.UserRepository;

// Runs on startup only when app.seed.enabled=true (set it in application-local.properties).
// Safe to run repeatedly: it skips users that already exist.
@Component
public class UserSeeder implements CommandLineRunner {

	private final AppProperties appProperties;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserSeeder(AppProperties appProperties, UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.appProperties = appProperties;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (!appProperties.seed().enabled()) {
			return;
		}
		seedAdmin();
	}

	private void seedAdmin() {
		String email = "admin@gmail.com";
		if (userRepository.existsByEmail(email)) {
			return;
		}
		var admin = new User();
		admin.setFirstName("Admin");
		admin.setLastName("User");
		admin.setEmail(email);
		admin.setPassword(passwordEncoder.encode("password"));
		admin.setRole(Role.ADMIN);
		userRepository.save(admin);
	}
}
