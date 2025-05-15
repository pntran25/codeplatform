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

import com.example.codeplatform.model.Submission;
import com.example.codeplatform.repository.SubmissionRepo;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionRepo submissionRepo;
    public SubmissionController(SubmissionRepo submissionRepo) {
        this.submissionRepo = submissionRepo;
    }

    @PostMapping
    public Submission createSubmission(@RequestBody Submission submission) {
        return submissionRepo.save(submission);
    }

    @GetMapping
    public List<Submission> getAllSubmissions() {
        return submissionRepo.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Submission> getSubmissionById(@PathVariable Long id) {
        return submissionRepo.findById(id);
    }

    @PutMapping("/{id}")
    public Submission updateSubmission(@PathVariable Long id, @RequestBody Submission updatedSubmission) {
        updatedSubmission.setId(id);
        return submissionRepo.save(updatedSubmission);
    }

    @DeleteMapping("/{id}")
    public void deleteSubmission(@PathVariable Long id) {
        submissionRepo.deleteById(id);
    }
}
