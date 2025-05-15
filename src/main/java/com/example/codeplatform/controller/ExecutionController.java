package com.example.codeplatform.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.codeplatform.model.Execution;
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
    public Execution executeCode(@RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        String input = (String) payload.getOrDefault("input", "");
        String language = (String) payload.getOrDefault("language", "python3");
        Long userId = payload.get("userId") != null ? Long.valueOf(payload.get("userId").toString()) : null;
        Long problemId = payload.get("problemId") != null ? Long.valueOf(payload.get("problemId").toString()) : null;
        Long submissionId = payload.get("submissionId") != null ? Long.valueOf(payload.get("submissionId").toString()) : null;

        Map<String, Object> result = jdoodleService.execute(code, input, language);

        Execution execution = new Execution();
        if (userId != null) execution.setUser(userRepo.findById(userId).orElse(null));
        if (problemId != null) execution.setProblem(problemRepo.findById(problemId).orElse(null));
        if (submissionId != null) execution.setSubmission(submissionRepo.findById(submissionId).orElse(null));
        execution.setLanguage(language);
        execution.setInput(input);
        execution.setOutput((String) result.get("output"));
        execution.setError((String) result.get("error"));
        execution.setSuccess(result.get("statusCode") != null && result.get("statusCode").equals(200));
        execution.setExecutedAt(LocalDateTime.now());

        return executionRepo.save(execution);
    }
}
