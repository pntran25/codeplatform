package com.example.codeplatform.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.service.ProblemService;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    // Create a new problem
    @PostMapping
    public Problem createProblem(@RequestBody Problem problem) {
        return problemService.saveProblem(problem);
    }

    // Get all problems
    @GetMapping
    public List<Problem> getAllProblems() {
        return problemService.getAllProblems();
    }

    // Get problem by ID
    @GetMapping("/{id}")
    public Optional<Problem> getProblemById(@PathVariable Long id) {
        return problemService.getProblemById(id);
    }

    // Delete a problem
    @DeleteMapping("/{id}")
    public void deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
    }
}
