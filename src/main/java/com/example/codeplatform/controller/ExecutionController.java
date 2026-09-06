package com.example.codeplatform.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.codeplatform.model.Execution;
import com.example.codeplatform.model.Problem;
import com.example.codeplatform.model.Submission;
import com.example.codeplatform.model.TestCase;
import com.example.codeplatform.repository.ExecutionRepo;
import com.example.codeplatform.repository.ProblemRepository;
import com.example.codeplatform.repository.SubmissionRepo;
import com.example.codeplatform.repository.UserRepo;
import com.example.codeplatform.service.JDoodleService;
import com.example.codeplatform.service.PythonHarness;
import com.example.codeplatform.service.SubmissionService;

@RestController
@RequestMapping("/api/execute")
public class ExecutionController {

    /** The harness generated for each run is Python-specific. */
    private static final String LANGUAGE = "python3";
    private static final String VERSION_INDEX = "4";

    private final JDoodleService jdoodleService;
    private final ExecutionRepo executionRepo;
    private final UserRepo userRepo;
    private final ProblemRepository problemRepo;
    private final SubmissionRepo submissionRepo;
    private final SubmissionService submissionService;

    public ExecutionController(JDoodleService jdoodleService, ExecutionRepo executionRepo,
                               UserRepo userRepo, ProblemRepository problemRepo, SubmissionRepo submissionRepo,
                               SubmissionService submissionService) {
        this.jdoodleService = jdoodleService;
        this.executionRepo = executionRepo;
        this.userRepo = userRepo;
        this.problemRepo = problemRepo;
        this.submissionRepo = submissionRepo;
        this.submissionService = submissionService;
    }

    /** Request body for a run. {@code submissionId} is optional. */
    public record ExecuteRequest(String functionBody, Long problemId, Long submissionId) {
    }

    @PostMapping
    public Map<String, Object> executeCode(@RequestBody ExecuteRequest request, Principal principal) {
        if (request.problemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "problemId is required");
        }
        Problem problem = problemRepo.findById(request.problemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Problem " + request.problemId() + " not found"));

        List<TestCase> testCases = problem.getTestCases() == null ? List.of() : problem.getTestCases();
        if (testCases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This problem has no test cases yet");
        }

        String functionName = PythonHarness.functionName(problem.getFunctionSignature());
        if (functionName == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Problem " + problem.getId() + " has an unusable function signature");
        }

        // Reject before calling the execution API so an invalid submission costs no quota.
        if (PythonHarness.callsPrint(request.functionBody())) {
            return rejected(testCases, "Do not use print in your function. Use return instead.");
        }

        String script = PythonHarness.buildScript(
                problem.getFunctionSignature(), request.functionBody(), functionName, testCases);
        Map<String, Object> jdoodleResult = jdoodleService.execute(script, "", LANGUAGE, VERSION_INDEX);

        String rawOutput = asString(jdoodleResult.get("output"));
        String error = asString(jdoodleResult.get("error"));
        List<String> blocks = PythonHarness.splitOutput(rawOutput, testCases.size());

        List<Map<String, Object>> caseResults = new ArrayList<>(testCases.size());
        for (int i = 0; i < testCases.size(); i++) {
            caseResults.add(evaluate(testCases.get(i), blocks.get(i)));
        }

        recordExecution(principal, problem, request.submissionId(), rawOutput, error, jdoodleResult);

        int passed = (int) caseResults.stream().filter(r -> Boolean.TRUE.equals(r.get("pass"))).count();
        boolean solved = false;
        if (principal != null) {
            // Persist the code that was run and update the user's progress on this problem.
            Submission submission = submissionService.recordRun(
                    principal.getName(), problem, request.functionBody(), passed, caseResults.size());
            solved = submission.isSolved();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", caseResults);
        response.put("passed", passed);
        response.put("total", caseResults.size());
        response.put("solved", solved);
        response.put("rawOutput", rawOutput);
        response.put("error", error);
        return response;
    }

    private Map<String, Object> evaluate(TestCase testCase, String actual) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("input", testCase.getInput());
        result.put("expected", testCase.getExpected());

        if (actual != null && actual.startsWith(PythonHarness.ERROR_MARKER)) {
            result.put("actual", null);
            result.put("pass", false);
            result.put("error", actual.substring(PythonHarness.ERROR_MARKER.length()).strip());
            return result;
        }

        result.put("actual", actual);
        String expectedNorm = PythonHarness.normalize(testCase.getExpected());
        String actualNorm = PythonHarness.normalize(actual);
        result.put("pass", expectedNorm != null && expectedNorm.equals(actualNorm));
        if (actual == null) {
            result.put("error", "No output produced for this case");
        }
        return result;
    }

    /** Builds a full failing result set without calling the execution service. */
    private Map<String, Object> rejected(List<TestCase> testCases, String message) {
        List<Map<String, Object>> caseResults = new ArrayList<>(testCases.size());
        for (TestCase tc : testCases) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("input", tc.getInput());
            result.put("expected", tc.getExpected());
            result.put("actual", null);
            result.put("pass", false);
            result.put("error", message);
            caseResults.add(result);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", caseResults);
        response.put("passed", 0);
        response.put("total", caseResults.size());
        response.put("solved", false);
        response.put("rawOutput", "");
        response.put("error", message);
        return response;
    }

    private void recordExecution(Principal principal, Problem problem, Long submissionId,
                                 String rawOutput, String error, Map<String, Object> jdoodleResult) {
        Execution execution = new Execution();
        // Attribute the run to the authenticated caller, not to an id supplied in the request body.
        if (principal != null) {
            userRepo.findByUsername(principal.getName()).ifPresent(execution::setUser);
        }
        execution.setProblem(problem);
        if (submissionId != null) {
            submissionRepo.findById(submissionId).ifPresent(execution::setSubmission);
        }
        execution.setLanguage(LANGUAGE);
        execution.setInput(""); // function problems pass arguments, not stdin
        execution.setOutput(rawOutput);
        execution.setError(error);
        Object statusCode = jdoodleResult.get("statusCode");
        execution.setSuccess(statusCode instanceof Number n && n.intValue() == 200);
        execution.setExecutedAt(LocalDateTime.now());
        executionRepo.save(execution);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
