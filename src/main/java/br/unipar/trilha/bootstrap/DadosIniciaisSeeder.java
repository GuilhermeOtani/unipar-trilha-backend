package br.unipar.trilha.bootstrap;

import br.unipar.trilha.entities.*;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DadosIniciaisSeeder implements org.springframework.boot.CommandLineRunner {
    private final UsuarioRepository usuarioRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final TurmaRepository turmaRepository;
    private final VinculoProfessorRepository vinculoRepository;
    private final MatriculaAlunoRepository matriculaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        criarUsuario("admin", "Administrador", "admin123", Perfil.ADMINISTRADOR);
        Usuario professor = criarUsuario("professor", "Professor Demo", "prof123", Perfil.PROFESSOR);
        Usuario aluno = criarUsuario("aluno", "Aluno Demo", "aluno123", Perfil.ALUNO);

        Disciplina disciplina = disciplinaRepository.findByCodigo("ALG-001")
                .orElseGet(() -> disciplinaRepository.save(Disciplina.builder()
                        .nome("Algoritmos e Lógica de Programação")
                        .codigo("ALG-001")
                        .ativo(true)
                        .build()));
        Turma turma = turmaRepository.findAll().stream()
                .filter(item -> item.getNome().equals("Turma Piloto") && item.getDisciplina().getId().equals(disciplina.getId()))
                .findFirst()
                .orElseGet(() -> turmaRepository.save(Turma.builder()
                        .nome("Turma Piloto")
                        .periodo("2026/2")
                        .disciplina(disciplina)
                        .ativo(true)
                        .build()));
        if (!vinculoRepository.existsByProfessorIdAndTurmaId(professor.getId(), turma.getId())) {
            vinculoRepository.save(VinculoProfessor.builder().professor(professor).turma(turma).build());
        }
        if (!matriculaRepository.existsByAlunoIdAndTurmaId(aluno.getId(), turma.getId())) {
            matriculaRepository.save(MatriculaAluno.builder().aluno(aluno).turma(turma).build());
        }
    }

    private Usuario criarUsuario(String login, String nome, String senha, Perfil perfil) {
        return usuarioRepository.findByLogin(login)
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .login(login)
                        .nome(nome)
                        .senha(passwordEncoder.encode(senha))
                        .perfil(perfil)
                        .ativo(true)
                        .build()));
    }
}
