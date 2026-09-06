package com.example.codeplatform.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.codeplatform.JwtUtil;
import com.example.codeplatform.model.User;
import com.example.codeplatform.repository.UserRepo;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Set<String> adminUsernames;

    public AuthController(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                          @Value("${app.admin-usernames:}") List<String> adminUsernames) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.adminUsernames = adminUsernames.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(name -> name.toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        String password = user.getPassword() == null ? "" : user.getPassword();

        if (!StringUtils.hasText(username)) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest()
                    .body("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (userRepo.existsByUsername(username)) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User created = new User();
        created.setUsername(username);
        created.setPassword(passwordEncoder.encode(password));
        created.setRole(adminUsernames.contains(username.toLowerCase(java.util.Locale.ROOT)) ? "ADMIN" : "USER");
        userRepo.save(created);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User credentials) {
        String username = credentials.getUsername() == null ? "" : credentials.getUsername().trim();
        Optional<User> found = userRepo.findByUsername(username);

        // Deliberately identical response for "no such user" and "wrong password" so the endpoint
        // cannot be used to enumerate accounts.
        if (found.isEmpty() || !passwordEncoder.matches(credentials.getPassword(), found.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        User user = found.get();
        return ResponseEntity.ok(Map.of(
                "token", jwtUtil.generateToken(user.getUsername(), user.getRole()),
                "username", user.getUsername(),
                "role", user.getRole()));
    }

    /** Returns the caller's identity, or 401 when the bearer token is missing or invalid. */
    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepo.findByUsername(principal.getName())
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "role", user.getRole())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
