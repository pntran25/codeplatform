package com.example.codeplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.codeplatform.model.Execution;

public interface ExecutionRepo extends JpaRepository<Execution, Long> {}