package com.example.codeplatform.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.model.Submission;
import com.example.codeplatform.model.User;
import com.example.codeplatform.repository.SubmissionRepo;
import com.example.codeplatform.repository.UserRepo;

/**
 * Owns the one-row-per-(user, problem) invariant so autosave and code execution both update the
 * same record instead of racing to create duplicates.
 */
@Service
public class SubmissionService {

    private final SubmissionRepo submissionRepo;
    private final UserRepo userRepo;

    public SubmissionService(SubmissionRepo submissionRepo, UserRepo userRepo) {
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
    }

    public List<Submission> listForUser(String username) {
        return submissionRepo.findByUserUsernameOrderByUpdatedAtDesc(username);
    }

    public Optional<Submission> find(String username, Long problemId) {
        return submissionRepo.findByUserUsernameAndProblemId(username, problemId);
    }

    /** Saves the user's draft without running it; never changes the solved flag. */
    @Transactional
    public Submission saveCode(String username, Problem problem, String code) {
        Submission submission = findOrCreate(username, problem);
        submission.setCode(code);
        submission.setUpdatedAt(LocalDateTime.now());
        return submissionRepo.save(submission);
    }

    /** Records a run's outcome; marks the problem solved once every case passes. */
    @Transactional
    public Submission recordRun(String username, Problem problem, String code, int passed, int total) {
        Submission submission = findOrCreate(username, problem);
        LocalDateTime now = LocalDateTime.now();
        submission.setCode(code);
        submission.setPassedCount(passed);
        submission.setTotalCount(total);
        submission.setLastRunAt(now);
        submission.setUpdatedAt(now);
        if (total > 0 && passed == total) {
            submission.setSolved(true);
        }
        return submissionRepo.save(submission);
    }

    private Submission findOrCreate(String username, Problem problem) {
        return submissionRepo.findByUserUsernameAndProblemId(username, problem.getId())
                .orElseGet(() -> {
                    User user = userRepo.findByUsername(username)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
                    Submission created = new Submission();
                    created.setUser(user);
                    created.setProblem(problem);
                    return created;
                });
    }
}
