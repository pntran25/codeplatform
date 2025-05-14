package com.example.codeplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.codeplatform.model.Submission;

public interface SubmissionRepo extends JpaRepository<Submission, Long> {}