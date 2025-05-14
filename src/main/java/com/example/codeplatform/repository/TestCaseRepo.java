package com.example.codeplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.codeplatform.model.TestCase;

public interface TestCaseRepo extends JpaRepository<TestCase, Long> {}