package com.example.codeplatform.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.repository.ProblemRepository;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    
    public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    // Save a new problem
    public Problem saveProblem(Problem problem) {
        return problemRepository.save(problem);
    }

    // Find a problem by its ID
    public Optional<Problem> getProblemById(Long id) {
        return problemRepository.findById(id);
    }

    // Get all problems
    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    // Delete a problem by ID
    public void deleteProblem(Long id) {
        problemRepository.deleteById(id);
    }
}