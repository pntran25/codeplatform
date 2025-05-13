package com.example.codeplatform.repository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.codeplatform.model.Problem;

@DataJpaTest
class ProblemRepositoryTest {
    @Autowired
    private ProblemRepository problemRepository;

    @Test
    void testSaveAndFind() {
        Problem p = new Problem();
        p.setTitle("Sample");
        p.setDescription("Desc");
        problemRepository.save(p);

        List<Problem> all = problemRepository.findAll();
        assertThat(all).isNotEmpty();
    }
}