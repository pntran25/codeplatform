package com.example.codeplatform.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.codeplatform.model.Execution;
import com.example.codeplatform.model.Problem;
import com.example.codeplatform.model.TestCase;
import com.example.codeplatform.repository.ExecutionRepo;
import com.example.codeplatform.repository.ProblemRepository;
import com.example.codeplatform.repository.SubmissionRepo;
import com.example.codeplatform.repository.UserRepo;
import com.example.codeplatform.service.JDoodleService;

@RestController
@RequestMapping("/api/execute")
public class ExecutionController {
    private final JDoodleService jdoodleService;
    private final ExecutionRepo executionRepo;
    private final UserRepo userRepo;
    private final ProblemRepository problemRepo;
    private final SubmissionRepo submissionRepo;

    public ExecutionController(JDoodleService jdoodleService, ExecutionRepo executionRepo,
                               UserRepo userRepo, ProblemRepository problemRepo, SubmissionRepo submissionRepo) {
        this.jdoodleService = jdoodleService;
        this.executionRepo = executionRepo;
        this.userRepo = userRepo;
        this.problemRepo = problemRepo;
        this.submissionRepo = submissionRepo;
    }

    @PostMapping
    public Map<String, Object> executeCode(@RequestBody Map<String, Object> payload) {
        String functionBody = (String) payload.get("functionBody");
        String language = (String) payload.getOrDefault("language", "python3");
        Long userId = payload.get("userId") != null ? Long.valueOf(payload.get("userId").toString()) : null;
        Long problemId = payload.get("problemId") != null ? Long.valueOf(payload.get("problemId").toString()) : null;
        Long submissionId = payload.get("submissionId") != null ? Long.valueOf(payload.get("submissionId").toString()) : null;

        if (problemId == null) {
             throw new IllegalArgumentException("problemId is required");
        }
        Problem problem = problemRepo.findById(problemId).orElseThrow();
        String functionSignature = problem.getFunctionSignature();
        List<TestCase> testCases = problem.getTestCases();

        String functionName = extractFunctionName(functionSignature);

        // Build the code to send to JDoodle
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append(functionSignature).append("\n");
        codeBuilder.append(functionBody).append("\n\n");
        for (TestCase tc : testCases) {
            codeBuilder.append("print(")
                .append(functionName)
                .append("(")
                .append(tc.getInputAsPythonArgs())
                .append("))\n");
        }
        String fullCode = codeBuilder.toString();

        // Send to JDoodle
        Map<String, Object> result = jdoodleService.execute(fullCode, "", language);

        // Parse output and compare to expected
        String rawOutput = (String) result.get("output");
        String[] outputs = rawOutput != null ? rawOutput.trim().split("\\r?\\n") : new String[0];
        List<Map<String, Object>> caseResults = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            Map<String, Object> r = new HashMap<>();
            r.put("input", testCases.get(i).getInput());
            r.put("expected", testCases.get(i).getExpected());
            r.put("actual", i < outputs.length ? outputs[i] : null);
            r.put("pass", i < outputs.length && outputs[i].equals(String.valueOf(testCases.get(i).getExpected())));
            caseResults.add(r);
        }

        // Save execution (optional, you can keep this logic)
        Execution execution = new Execution();
        if (userId != null) execution.setUser(userRepo.findById(userId).orElse(null));
        execution.setProblem(problemRepo.findById(problemId).orElse(null));
        if (submissionId != null) execution.setSubmission(submissionRepo.findById(submissionId).orElse(null));
        execution.setLanguage(language);
        execution.setInput(""); // No stdin input for function problems
        execution.setOutput(rawOutput);
        execution.setError((String) result.get("error"));
        execution.setSuccess(result.get("statusCode") != null && result.get("statusCode").equals(200));
        execution.setExecutedAt(LocalDateTime.now());
        executionRepo.save(execution);

        // Return results for frontend
        Map<String, Object> response = new HashMap<>();
        response.put("results", caseResults);
        response.put("rawOutput", rawOutput);
        response.put("error", result.get("error"));
        return response;
    }

    // Helper method to extract function name from signature
    private String extractFunctionName(String signature) {
        // e.g., "def add(a, b):" -> "add"
        try {
            String trimmed = signature.trim();
            if (trimmed.startsWith("def ")) {
                int start = 4;
                int end = trimmed.indexOf('(');
                if (end > start) {
                    return trimmed.substring(start, end).trim();
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return "func";
    }
}