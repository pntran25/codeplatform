package com.example.codeplatform.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    // Create a new problem, optionally with its test cases inline
    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public Problem createProblem(@RequestBody Problem problem) {
        validate(problem);
        return problemService.create(problem);
    }

    // Get all problems
    @GetMapping
    public List<Problem> getAllProblems() {
        return problemService.getAllProblems();
    }

    // Get problem by ID
    @GetMapping("/{id}")
    public Problem getProblemById(@PathVariable Long id) {
        return requireProblem(id);
    }

    // Update a problem; a non-null testCases array replaces the existing cases
    @PutMapping("/{id}")
    public Problem updateProblem(@PathVariable Long id, @RequestBody Problem updatedProblem) {
        validate(updatedProblem);
        return problemService.update(id, updatedProblem);
    }

    // Delete a problem
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(@PathVariable Long id) {
        requireProblem(id);
        problemService.deleteProblem(id);
        return ResponseEntity.noContent().build();
    }

    // Add test case to a problem
    @PostMapping("/{problemId}/testcases")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public TestCase addTestCaseToProblem(@PathVariable Long problemId, @RequestBody TestCase testCase) {
        testCase.setId(null);
        testCase.setProblem(requireProblem(problemId));
        return testCaseRepo.save(testCase);
    }

    // Update a test case for a problem
    @PutMapping("/{problemId}/testcases/{testCaseId}")
    public TestCase updateTestCaseForProblem(@PathVariable Long problemId, @PathVariable Long testCaseId,
                                             @RequestBody TestCase updatedTestCase) {
        Problem problem = requireProblem(problemId);
        requireTestCaseOf(problem, testCaseId);
        updatedTestCase.setId(testCaseId);
        updatedTestCase.setProblem(problem);
        return testCaseRepo.save(updatedTestCase);
    }

    // Delete a test case for a problem
    @DeleteMapping("/{problemId}/testcases/{testCaseId}")
    public ResponseEntity<Void> deleteTestCaseForProblem(@PathVariable Long problemId, @PathVariable Long testCaseId) {
        requireTestCaseOf(requireProblem(problemId), testCaseId);
        testCaseRepo.deleteById(testCaseId);
        return ResponseEntity.noContent().build();
    }

    private static void validate(Problem problem) {
        if (problem.getTitle() == null || problem.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (com.example.codeplatform.service.PythonHarness.functionName(problem.getFunctionSignature()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Function signature must look like \"def name(args):\"");
        }
        if (problem.getDifficulty() != null && !problem.getDifficulty().isBlank()) {
            String d = problem.getDifficulty().trim().toUpperCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("EASY", "MEDIUM", "HARD").contains(d)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty must be EASY, MEDIUM or HARD");
            }
            problem.setDifficulty(d);
        } else {
            problem.setDifficulty(null);
        }
    }

    private Problem requireProblem(Long id) {
        return problemService.getProblemById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem " + id + " not found"));
    }

    /** Guards against editing a test case through a problem it does not belong to. */
    private TestCase requireTestCaseOf(Problem problem, Long testCaseId) {
        return testCaseRepo.findById(testCaseId)
                .filter(tc -> tc.getProblem() != null && tc.getProblem().getId().equals(problem.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Test case " + testCaseId + " not found for problem " + problem.getId()));
    }
}
