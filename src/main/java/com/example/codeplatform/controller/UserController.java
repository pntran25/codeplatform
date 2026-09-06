package com.example.codeplatform.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.codeplatform.model.User;
import com.example.codeplatform.repository.UserRepo;

/**
 * Admin-only user administration. Account creation goes through
 * {@link AuthController#register} so that passwords are always hashed and validated in one place.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return requireUser(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        User existing = requireUser(id);
        if (StringUtils.hasText(updatedUser.getUsername())) {
            existing.setUsername(updatedUser.getUsername().trim());
        }
        if (StringUtils.hasText(updatedUser.getRole())) {
            existing.setRole(updatedUser.getRole());
        }
        // Re-hash rather than storing whatever the client sent.
        if (StringUtils.hasText(updatedUser.getPassword())) {
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepo.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        requireUser(id);
        userRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private User requireUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + id + " not found"));
    }
}
