package com.example.codeplatform.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.codeplatform.model.Submission;

public interface SubmissionRepo extends JpaRepository<Submission, Long> {

    List<Submission> findByUserUsernameOrderByUpdatedAtDesc(String username);

    // Explicit JPQL: Submission exposes a getProblemId() convenience getter, which would make the
    // derived-query parser resolve "ProblemId" to a non-existent attribute instead of problem.id.
    @Query("select s from Submission s where s.user.username = :username and s.problem.id = :problemId")
    Optional<Submission> findByUserUsernameAndProblemId(@Param("username") String username,
                                                        @Param("problemId") Long problemId);
}
