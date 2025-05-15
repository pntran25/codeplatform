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

import com.example.codeplatform.model.TestCase;
import com.example.codeplatform.repository.TestCaseRepo;

@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {
    private final TestCaseRepo testCaseRepo;
    public TestCaseController(TestCaseRepo testCaseRepo) {
        this.testCaseRepo = testCaseRepo;
    }

    @PostMapping
    public TestCase createTestCase(@RequestBody TestCase testCase) {
        return testCaseRepo.save(testCase);
    }

    @GetMapping
    public List<TestCase> getAllTestCases() {
        return testCaseRepo.findAll();
    }

    @GetMapping("/{id}")
    public Optional<TestCase> getTestCaseById(@PathVariable Long id) {
        return testCaseRepo.findById(id);
    }

    @PutMapping("/{id}")
    public TestCase updateTestCase(@PathVariable Long id, @RequestBody TestCase updatedTestCase) {
        updatedTestCase.setId(id);
        return testCaseRepo.save(updatedTestCase);
    }

    @DeleteMapping("/{id}")
    public void deleteTestCase(@PathVariable Long id) {
        testCaseRepo.deleteById(id);
    }
}
