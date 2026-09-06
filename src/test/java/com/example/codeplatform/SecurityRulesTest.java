package com.example.codeplatform;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.example.codeplatform.repository.UserRepo;
import com.example.codeplatform.service.JDoodleService;

/**
 * Pins the authorization rules that used to be missing: problem authoring, test-case editing and
 * user administration were all reachable without any credentials.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepo userRepo;

    @MockitoBean
    private JDoodleService jdoodleService;

    @Test
    void anyoneCanBrowseProblems() throws Exception {
        mockMvc.perform(get("/api/problems")).andExpect(status().isOk());
    }

    @Test
    void anonymousCannotCreateProblems() throws Exception {
        mockMvc.perform(post("/api/problems").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ordinaryUsersCannotCreateProblems() throws Exception {
        mockMvc.perform(post("/api/problems").with(user("bob").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordinaryUsersCannotListUsersOrEditTestCases() throws Exception {
        mockMvc.perform(get("/api/users").with(user("bob").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/testcases/1").with(user("bob").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotRunCode() throws Exception {
        mockMvc.perform(post("/api/execute").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemId\":1,\"functionBody\":\"    return 1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidatesInputAndNeverEchoesThePasswordHash() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"correct horse battery\"}"))
                .andExpect(status().isCreated());

        // Duplicate usernames are rejected.
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"correct horse battery\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());

        userRepo.deleteAll();
    }

    @Test
    void loginWithBadCredentialsDoesNotRevealWhetherTheUserExists() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsAUsableToken() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"correct horse battery\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"correct horse battery\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"));

        userRepo.deleteAll();
    }
}
