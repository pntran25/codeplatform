package com.example.codeplatform.model;

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

    private String input;      // Store as JSON string or comma-separated values
    private String expected;   // Store as string for simplicity

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    // Helper to convert input string to Python args (e.g., "2,3")
    public String getInputAsPythonArgs() {
        return input; // If stored as "2,3", just return input
    }
}
