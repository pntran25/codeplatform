package com.example.codeplatform.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.codeplatform.model.Problem;
import com.example.codeplatform.repository.ProblemRepository;

@ExtendWith(MockitoExtension.class)
public class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private ProblemService problemService;

    @Test
    public void testGetProblemById() {
        Problem problem = new Problem();
        problem.setTitle("Test Problem");
        problem.setDescription("Test Description");

        // Mock the repository to return the problem when findById is called
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

        // Call the service method
        Optional<Problem> foundOptional = problemService.getProblemById(1L);

        // Assertions
        assertThat(foundOptional).isPresent();
        Problem foundProblem = foundOptional.get();
        assertThat(foundProblem.getTitle()).isEqualTo("Test Problem");
        assertThat(foundProblem.getDescription()).isEqualTo("Test Description");
    }
}
