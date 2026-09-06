package com.example.codeplatform.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.model.Submission;
import com.example.codeplatform.repository.ProblemRepository;
import com.example.codeplatform.service.SubmissionService;

/**
 * The caller's saved work. Every route is scoped to the authenticated user, so there is no way
 * to read or overwrite someone else's code.
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ProblemRepository problemRepo;

    public SubmissionController(SubmissionService submissionService, ProblemRepository problemRepo) {
        this.submissionService = submissionService;
        this.problemRepo = problemRepo;
    }

    /** Body for autosave. */
    public record SaveCodeRequest(String code) {
    }

    /** Everything the caller has worked on, most recent first - drives the progress view. */
    @GetMapping("/mine")
    public List<Submission> mine(Principal principal) {
        return submissionService.listForUser(principal.getName());
    }

    /** The caller's saved draft for one problem, or 404 if they have not started it. */
    @GetMapping("/problem/{problemId}")
    public Submission forProblem(@PathVariable Long problemId, Principal principal) {
        return submissionService.find(principal.getName(), problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No saved code yet"));
    }

    /** Autosave. Idempotent: creates the row on first call, updates it after. */
    @PutMapping("/problem/{problemId}")
    public Submission saveForProblem(@PathVariable Long problemId, @RequestBody SaveCodeRequest body,
                                     Principal principal) {
        Problem problem = problemRepo.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Problem " + problemId + " not found"));
        return submissionService.saveCode(principal.getName(), problem, body.code() == null ? "" : body.code());
    }
}
