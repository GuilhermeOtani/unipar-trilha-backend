package br.unipar.trilha;

import br.unipar.trilha.entities.*;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CicloCompletoIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired DisciplinaRepository disciplinaRepository;
    @Autowired TurmaRepository turmaRepository;
    @Autowired VinculoProfessorRepository vinculoRepository;
    @Autowired MatriculaAlunoRepository matriculaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Usuario professor;
    private Usuario aluno;
    private Disciplina disciplina;
    private Turma turma;

    @BeforeEach
    void prepararContextoAcademico() {
        professor = usuarioRepository.save(Usuario.builder()
                .login("professor.teste").nome("Professor Teste")
                .senha(passwordEncoder.encode("prof123")).perfil(Perfil.PROFESSOR).ativo(true).build());
        aluno = usuarioRepository.save(Usuario.builder()
                .login("aluno.teste").nome("Aluno Teste")
                .senha(passwordEncoder.encode("aluno123")).perfil(Perfil.ALUNO).ativo(true).build());
        disciplina = disciplinaRepository.save(Disciplina.builder()
                .nome("Algoritmos").codigo("ALG-TESTE").ativo(true).build());
        turma = turmaRepository.save(Turma.builder()
                .nome("Turma Teste").periodo("2026/2").disciplina(disciplina).ativo(true).build());
        vinculoRepository.save(VinculoProfessor.builder().professor(professor).turma(turma).build());
        matriculaRepository.save(MatriculaAluno.builder().aluno(aluno).turma(turma).build());
    }

    @Test
    void professorPublicaAlunoPraticaEProfessorAcompanha() throws Exception {
        String tokenProfessor = login("professor.teste", "prof123");

        mockMvc.perform(get("/professor/contexto").header("Authorization", bearer(tokenProfessor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turmas[0].turmaId").value(turma.getId()));

        String rascunhoJson = mockMvc.perform(post("/trilhas")
                        .header("Authorization", bearer(tokenProfessor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Fundamentos da lógica","descricao":"Trilha piloto","disciplinaId":%d}
                                """.formatted(disciplina.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn().getResponse().getContentAsString();
        long trilhaId = objectMapper.readTree(rascunhoJson).get("id").asLong();

        mockMvc.perform(put("/trilhas/{id}", trilhaId)
                        .header("Authorization", bearer(tokenProfessor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(arvoreCompleta(disciplina.getId(), "Fundamentos da lógica")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulos[0].licoes[0].desafios.length()").value(2));

        String publicacaoJson = mockMvc.perform(post("/trilhas/{id}/publicacoes", trilhaId)
                        .header("Authorization", bearer(tokenProfessor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroVersao").value(1))
                .andReturn().getResponse().getContentAsString();
        long versaoId = objectMapper.readTree(publicacaoJson).get("versaoId").asLong();

        String distribuicaoJson = mockMvc.perform(post("/distribuicoes")
                        .header("Authorization", bearer(tokenProfessor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"versaoId":%d,"turmaId":%d,"disponivelDe":"%s","disponivelAte":null}
                                """.formatted(versaoId, turma.getId(), LocalDateTime.now().minusMinutes(1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long distribuicaoId = objectMapper.readTree(distribuicaoJson).get("id").asLong();

        String tokenAluno = login("aluno.teste", "aluno123");
        String catalogo = mockMvc.perform(get("/aluno/distribuicoes").header("Authorization", bearer(tokenAluno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribuicoes[0].distribuicaoId").value(distribuicaoId))
                .andReturn().getResponse().getContentAsString();
        assertThat(catalogo).doesNotContain("correta");

        String sessaoJson = mockMvc.perform(post("/aluno/distribuicoes/{id}/sessoes", distribuicaoId)
                        .header("Authorization", bearer(tokenAluno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progresso.percentual").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode sessao = objectMapper.readTree(sessaoJson);
        long sessaoId = sessao.get("sessaoId").asLong();
        long primeiroDesafio = sessao.at("/desafioAtual/id").asLong();
        long opcaoCorreta = sessao.at("/desafioAtual/opcoes/0/id").asLong();
        long opcaoErrada = sessao.at("/desafioAtual/opcoes/1/id").asLong();

        mockMvc.perform(post("/aluno/sessoes/{id}/respostas", sessaoId)
                        .header("Authorization", bearer(tokenAluno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resposta(primeiroDesafio, opcaoErrada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(false))
                .andExpect(jsonPath("$.progresso.percentual").value(0));

        String primeiraCorreta = mockMvc.perform(post("/aluno/sessoes/{id}/respostas", sessaoId)
                        .header("Authorization", bearer(tokenAluno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resposta(primeiroDesafio, opcaoCorreta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(true))
                .andExpect(jsonPath("$.progresso.percentual").value(50))
                .andReturn().getResponse().getContentAsString();
        JsonNode proximo = objectMapper.readTree(primeiraCorreta).get("proximoDesafio");

        mockMvc.perform(get("/aluno/sessoes/{id}", sessaoId).header("Authorization", bearer(tokenAluno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desafioAtual.id").value(proximo.get("id").asLong()));

        mockMvc.perform(post("/aluno/sessoes/{id}/respostas", sessaoId)
                        .header("Authorization", bearer(tokenAluno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resposta(proximo.get("id").asLong(), proximo.at("/opcoes/0/id").asLong())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progresso.percentual").value(100))
                .andExpect(jsonPath("$.progresso.concluida").value(true));

        mockMvc.perform(get("/professor/turmas/{id}/indicadores", turma.getId())
                        .header("Authorization", bearer(tokenProfessor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iniciaram").value(1))
                .andExpect(jsonPath("$.concluiram").value(1))
                .andExpect(jsonPath("$.erros").value(1));

        mockMvc.perform(put("/trilhas/{id}", trilhaId)
                        .header("Authorization", bearer(tokenProfessor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(arvoreCompleta(disciplina.getId(), "Fundamentos atualizados")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/trilhas/{id}/publicacoes", trilhaId)
                        .header("Authorization", bearer(tokenProfessor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroVersao").value(2));

        mockMvc.perform(get("/professor/turmas/{id}/indicadores", turma.getId())
                        .header("Authorization", bearer(tokenProfessor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concluiram").value(1))
                .andExpect(jsonPath("$.desafiosComMaisErros[0].numeroVersao").value(1));
    }

    @Test
    void alunoNaoAcessaRecursosDoProfessor() throws Exception {
        String tokenAluno = login("aluno.teste", "aluno123");
        mockMvc.perform(get("/professor/contexto").header("Authorization", bearer(tokenAluno)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void autenticacaoEValidacaoRetornamProblemDetail() throws Exception {
        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não autenticado"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"aluno.teste\",\"senha\":\"incorreta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas."));

        String tokenProfessor = login("professor.teste", "prof123");
        mockMvc.perform(post("/trilhas")
                        .header("Authorization", bearer(tokenProfessor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"disciplinaId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.titulo").exists())
                .andExpect(jsonPath("$.errors.disciplinaId").exists());
    }

    private String login(String login, String senha) throws Exception {
        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String resposta(long desafioId, long opcaoId) {
        return "{\"desafioId\":%d,\"opcaoId\":%d}".formatted(desafioId, opcaoId);
    }

    private String arvoreCompleta(long disciplinaId, String titulo) {
        return """
                {
                  "titulo":"%s",
                  "descricao":"Trilha piloto",
                  "disciplinaId":%d,
                  "modulos":[{
                    "titulo":"Condicionais","ordem":1,
                    "licoes":[{
                      "titulo":"If e else","resumo":"Escolhendo caminhos","ordem":1,
                      "desafios":[
                        {
                          "enunciado":"A condição é verdadeira?","tipo":"MULTIPLA_ESCOLHA",
                          "dificuldade":"FACIL","explicacao":"A condição é verdadeira.","ordem":1,
                          "opcoes":[
                            {"texto":"Sim","ordem":1,"correta":true},
                            {"texto":"Não","ordem":2,"correta":false}
                          ]
                        },
                        {
                          "enunciado":"Qual ramo será executado?","tipo":"MULTIPLA_ESCOLHA",
                          "dificuldade":"FACIL","explicacao":"O ramo if será executado.","ordem":2,
                          "opcoes":[
                            {"texto":"if","ordem":1,"correta":true},
                            {"texto":"else","ordem":2,"correta":false}
                          ]
                        }
                      ]
                    }]
                  }]
                }
                """.formatted(titulo, disciplinaId);
    }
}
