package com.example.codeplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "problem_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Problem problem;

    /** Comma-separated Python argument list, e.g. {@code "2,3"} or {@code "[1,2,3]"}. */
    @Column(columnDefinition = "TEXT")
    private String input;

    /** Expected value as printed by Python, e.g. {@code "5"}. */
    @Column(columnDefinition = "TEXT")
    private String expected;

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
}
