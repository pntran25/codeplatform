package com.example.codeplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.codeplatform.model.User;

public interface UserRepo extends JpaRepository<User, Long> {}