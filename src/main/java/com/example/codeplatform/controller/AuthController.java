package com.example.codeplatform.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        Optional<User> existing = userRepo.findAll().stream()
                .filter(u -> u.getUsername().equals(user.getUsername()))
                .findFirst();
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Only username 'pntran25' is admin
        if (user.getUsername().equalsIgnoreCase("pntran25")) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }
        userRepo.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> found = userRepo.findAll().stream()
                .filter(u -> u.getUsername().equals(user.getUsername()))
                .findFirst();
        if (found.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        if (!passwordEncoder.matches(user.getPassword(), found.get().getPassword())) {
            return ResponseEntity.status(401).body("Invalid password");
        }
    String token = JwtUtil.generateToken(found.get().getUsername(), found.get().getRole());       
    return ResponseEntity.ok(java.util.Collections.singletonMap("token", token));
    }
}
