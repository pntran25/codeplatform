package com.example.codeplatform.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    /** EASY, MEDIUM or HARD; optional. */
    private String difficulty;

    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String functionSignature;

    @Column(columnDefinition = "TEXT")
    private String starterCode;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<TestCase> testCases;

    /** Run history for this problem. Never part of the problem API payload. */
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Execution> executions;

    // Getter for id
    public Long getId() {
        return id;
    }

    // Setter for id
    public void setId(Long id) {
        this.id = id;
    }

    // Getter for title
    public String getTitle() {
        return title;
    }

    // Setter for title
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    // Getter for description
    public String getDescription() {
        return description;
    }

    // Setter for description
    public void setDescription(String description) {
        this.description = description;
    }

    public String getFunctionSignature() { return functionSignature; }
    public void setFunctionSignature(String functionSignature) { this.functionSignature = functionSignature; }

    public List<TestCase> getTestCases() {
        return testCases;
    }
    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    /**
     * Swaps the test cases in place. Mutating the managed collection (rather than assigning a new
     * one) is what lets Hibernate's orphanRemoval delete the cases that are no longer present.
     */
    public void replaceTestCases(List<TestCase> replacements) {
        if (this.testCases == null) {
            this.testCases = new java.util.ArrayList<>();
        }
        this.testCases.clear();
        if (replacements != null) {
            for (TestCase tc : replacements) {
                tc.setId(null);
                tc.setProblem(this);
                this.testCases.add(tc);
            }
        }
    }

    public String getStarterCode() {
        return starterCode;
    }

    public void setStarterCode(String starterCode) {
        this.starterCode = starterCode;
    }

    public List<Execution> getExecutions() {
        return executions;
    }
    public void setExecutions(List<Execution> executions) {
        this.executions = executions;
    }
}