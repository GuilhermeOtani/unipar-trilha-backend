package br.unipar.trilha.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new IntegridadeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void violacaoDeIntegridadeRetorna409ProblemDetail() throws Exception {
        mockMvc.perform(post("/teste/integridade"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflito de integridade"))
                .andExpect(jsonPath("$.detail")
                        .value("A operação viola uma restrição de integridade ou duplicidade."));
    }

    @RestController
    static class IntegridadeController {

        @PostMapping("/teste/integridade")
        void simularViolacao() {
            throw new DataIntegrityViolationException("restrição simulada");
        }
    }
}
