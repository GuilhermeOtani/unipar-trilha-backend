package br.unipar.trilha;

import br.unipar.trilha.bootstrap.DadosIniciaisSeeder;
import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.repositories.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DadosIniciaisSeederIntegrationTest {
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired TurmaRepository turmas;
    @Autowired VinculoProfessorRepository vinculos;
    @Autowired MatriculaAlunoRepository matriculas;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager entityManager;

    @Test
    void executarSeedDuasVezesPreservaRegistrosVinculosESenhas() {
        assertThat(usuarios.count()).isZero();
        assertThat(disciplinas.count()).isZero();
        assertThat(turmas.count()).isZero();
        assertThat(vinculos.count()).isZero();
        assertThat(matriculas.count()).isZero();

        // O bean automático pertence a dev; aqui executamos sua implementação real
        // dentro da transação de teste, usando somente o H2 e rollback ao terminar.
        var seeder = new DadosIniciaisSeeder(usuarios, disciplinas, turmas, vinculos, matriculas, encoder);
        seeder.run();
        entityManager.flush();
        entityManager.clear();

        assertThat(usuarios.count()).isEqualTo(3);
        assertThat(disciplinas.count()).isEqualTo(1);
        assertThat(turmas.count()).isEqualTo(1);
        assertThat(vinculos.count()).isEqualTo(1);
        assertThat(matriculas.count()).isEqualTo(1);

        Usuario admin = usuario("admin", "admin123", Perfil.ADMINISTRADOR);
        Usuario professor = usuario("professor", "prof123", Perfil.PROFESSOR);
        Usuario aluno = usuario("aluno", "aluno123", Perfil.ALUNO);
        var senhas = Map.of("admin", admin.getSenha(), "professor", professor.getSenha(), "aluno", aluno.getSenha());
        var idsUsuarios = usuarios.findAll().stream().map(Usuario::getId).toList();
        var disciplina = disciplinas.findByCodigo("ALG-001").orElseThrow();
        var turma = turmas.findAll().getFirst();
        var vinculo = vinculos.findAll().getFirst();
        var matricula = matriculas.findAll().getFirst();
        assertThat(turma.getDisciplina().getId()).isEqualTo(disciplina.getId());
        assertThat(vinculo.getProfessor().getId()).isEqualTo(professor.getId());
        assertThat(vinculo.getTurma().getId()).isEqualTo(turma.getId());
        assertThat(matricula.getAluno().getId()).isEqualTo(aluno.getId());
        assertThat(matricula.getTurma().getId()).isEqualTo(turma.getId());

        seeder.run();
        entityManager.flush();
        entityManager.clear();

        assertThat(usuarios.findAll()).extracting(Usuario::getId).containsExactlyInAnyOrderElementsOf(idsUsuarios);
        assertThat(disciplinas.findAll()).extracting(item -> item.getId()).containsExactly(disciplina.getId());
        assertThat(turmas.findAll()).extracting(item -> item.getId()).containsExactly(turma.getId());
        assertThat(vinculos.findAll()).extracting(item -> item.getId()).containsExactly(vinculo.getId());
        assertThat(matriculas.findAll()).extracting(item -> item.getId()).containsExactly(matricula.getId());
        for (Usuario salvo : usuarios.findAll()) {
            assertThat(salvo.getSenha()).isEqualTo(senhas.get(salvo.getLogin()));
        }
        assertThat(vinculos.findAll().getFirst().getProfessor().getId()).isEqualTo(professor.getId());
        assertThat(vinculos.findAll().getFirst().getTurma().getId()).isEqualTo(turma.getId());
        assertThat(matriculas.findAll().getFirst().getAluno().getId()).isEqualTo(aluno.getId());
        assertThat(matriculas.findAll().getFirst().getTurma().getId()).isEqualTo(turma.getId());
    }

    private Usuario usuario(String login, String senha, Perfil perfil) {
        Usuario usuario = usuarios.findByLogin(login).orElseThrow();
        assertThat(usuario.getPerfil()).isEqualTo(perfil);
        assertThat(usuario.getAtivo()).isTrue();
        assertThat(encoder.matches(senha, usuario.getSenha())).isTrue();
        return usuario;
    }
}
