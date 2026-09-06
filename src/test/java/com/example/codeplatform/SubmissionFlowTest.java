package com.example.codeplatform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.model.TestCase;
import com.example.codeplatform.repository.ProblemRepository;
import com.example.codeplatform.repository.SubmissionRepo;
import com.example.codeplatform.repository.UserRepo;
import com.example.codeplatform.service.JDoodleService;
import com.example.codeplatform.service.PythonHarness;

/** Saved code and solved status: one row per (user, problem), private to that user. */
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProblemRepository problemRepo;
    @Autowired
    private SubmissionRepo submissionRepo;
    @Autowired
    private UserRepo userRepo;

    @MockitoBean
    private JDoodleService jdoodleService;

    private Long problemId;

    @BeforeEach
    void setUp() throws Exception {
        Problem problem = new Problem();
        problem.setTitle("Add");
        problem.setFunctionSignature("def add(a, b):");
        TestCase tc = new TestCase();
        tc.setInput("2,3");
        tc.setExpected("5");
        problem.setTestCases(new java.util.ArrayList<>());
        problem.replaceTestCases(List.of(tc));
        problemId = problemRepo.save(problem).getId();

        for (String name : List.of("flow_user", "flow_other")) {
            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + name + "\",\"password\":\"correct horse battery\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @AfterEach
    void tearDown() {
        submissionRepo.deleteAll();
        problemRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void autosaveCreatesThenUpdatesASingleRowThatOnlyItsOwnerCanRead() throws Exception {
        mockMvc.perform(get("/api/submissions/problem/" + problemId).with(user("flow_user")))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/submissions/problem/" + problemId).with(user("flow_user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"    pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("    pass"))
                .andExpect(jsonPath("$.solved").value(false));

        mockMvc.perform(put("/api/submissions/problem/" + problemId).with(user("flow_user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"    return a + b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("    return a + b"));

        mockMvc.perform(get("/api/submissions/mine").with(user("flow_user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].problemId").value(problemId));

        // Someone else sees nothing for this problem.
        mockMvc.perform(get("/api/submissions/problem/" + problemId).with(user("flow_other")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/submissions/mine").with(user("flow_other")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aFullyPassingRunMarksTheProblemSolvedAndItStaysSolved() throws Exception {
        when(jdoodleService.execute(any(), any(), any(), any()))
                .thenReturn(Map.of("output", PythonHarness.CASE_MARKER + "\n5\n", "statusCode", 200));

        mockMvc.perform(post("/api/execute").with(user("flow_user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemId\":" + problemId + ",\"functionBody\":\"    return a + b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.solved").value(true));

        // A later failing run keeps the code but does not un-solve the problem.
        when(jdoodleService.execute(any(), any(), any(), any()))
                .thenReturn(Map.of("output", PythonHarness.CASE_MARKER + "\n99\n", "statusCode", 200));

        mockMvc.perform(post("/api/execute").with(user("flow_user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemId\":" + problemId + ",\"functionBody\":\"    return 99\"}"))
                .andExpect(jsonPath("$.passed").value(0))
                .andExpect(jsonPath("$.solved").value(true));

        mockMvc.perform(get("/api/submissions/problem/" + problemId).with(user("flow_user")))
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.code").value("    return 99"))
                .andExpect(jsonPath("$.passedCount").value(0))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void adminCanAuthorAProblemWithInlineTestCasesAndReplaceThem() throws Exception {
        String created = mockMvc.perform(post("/api/problems").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mul\",\"difficulty\":\"easy\",\"functionSignature\":\"def mul(a, b):\","
                                + "\"testCases\":[{\"input\":\"2,3\",\"expected\":\"6\"},{\"input\":\"0,9\",\"expected\":\"0\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.testCases.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/problems/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Multiply\",\"functionSignature\":\"def mul(a, b):\","
                                + "\"testCases\":[{\"input\":\"4,4\",\"expected\":\"16\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Multiply"))
                .andExpect(jsonPath("$.testCases.length()").value(1))
                .andExpect(jsonPath("$.testCases[0].input").value("4,4"));

        mockMvc.perform(post("/api/problems").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"functionSignature\":\"not a def\"}"))
                .andExpect(status().isBadRequest());
    }
}
