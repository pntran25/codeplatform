package com.example.codeplatform.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.model.TestCase;
import com.example.codeplatform.repository.TestCaseRepo;
import com.example.codeplatform.service.ProblemService;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final TestCaseRepo testCaseRepo;

    public ProblemController(ProblemService problemService, TestCaseRepo testCaseRepo) {
        this.problemService = problemService;
        this.testCaseRepo = testCaseRepo;
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

    // Update a problem
    @PutMapping("/{id}")
    public Problem updateProblem(@PathVariable Long id, @RequestBody Problem updatedProblem) {
        updatedProblem.setId(id);
        return problemService.saveProblem(updatedProblem);
    }

    // Delete a problem
    @DeleteMapping("/{id}")
    public void deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
    }

    // Add test case to a problem
    @PostMapping("/{problemId}/testcases")
    public TestCase addTestCaseToProblem(@PathVariable Long problemId, @RequestBody TestCase testCase) {
        Problem problem = problemService.getProblemById(problemId).orElseThrow();
        testCase.setProblem(problem);
        return testCaseRepo.save(testCase);
    }

    // Update a test case for a problem
    @PutMapping("/{problemId}/testcases/{testCaseId}")
    public TestCase updateTestCaseForProblem(@PathVariable Long problemId, @PathVariable Long testCaseId, @RequestBody TestCase updatedTestCase) {
        Problem problem = problemService.getProblemById(problemId).orElseThrow();
        updatedTestCase.setId(testCaseId);
        updatedTestCase.setProblem(problem);
        return testCaseRepo.save(updatedTestCase);
    }

    // Delete a test case for a problem
    @DeleteMapping("/{problemId}/testcases/{testCaseId}")
    public void deleteTestCaseForProblem(@PathVariable Long problemId, @PathVariable Long testCaseId) {
        testCaseRepo.deleteById(testCaseId);
    }
}
