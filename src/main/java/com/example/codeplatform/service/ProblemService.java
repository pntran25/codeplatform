package com.example.codeplatform.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.model.TestCase;
import com.example.codeplatform.repository.ProblemRepository;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;

    public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    /** Creates a problem together with any inline test cases. */
    @Transactional
    public Problem create(Problem problem) {
        problem.setId(null);
        List<TestCase> cases = problem.getTestCases();
        problem.setTestCases(new java.util.ArrayList<>());
        problem.replaceTestCases(cases);
        return problemRepository.save(problem);
    }

    /**
     * Updates scalar fields and, when {@code testCases} is non-null, replaces the whole test-case
     * list. Passing {@code null} leaves the existing cases untouched.
     */
    @Transactional
    public Problem update(Long id, Problem changes) {
        Problem existing = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem " + id + " not found"));
        existing.setTitle(changes.getTitle());
        existing.setDifficulty(changes.getDifficulty());
        existing.setDescription(changes.getDescription());
        existing.setFunctionSignature(changes.getFunctionSignature());
        existing.setStarterCode(changes.getStarterCode());
        if (changes.getTestCases() != null) {
            existing.replaceTestCases(changes.getTestCases());
        }
        return problemRepository.save(existing);
    }

    public Problem saveProblem(Problem problem) {
        return problemRepository.save(problem);
    }

    public Optional<Problem> getProblemById(Long id) {
        return problemRepository.findById(id);
    }

    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    public void deleteProblem(Long id) {
        problemRepository.deleteById(id);
    }
}
