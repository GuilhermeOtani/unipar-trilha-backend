package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.MatriculaAluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatriculaAlunoRepository extends JpaRepository<MatriculaAluno, Long> {
    List<MatriculaAluno> findByAlunoId(Long alunoId);
    boolean existsByAlunoIdAndTurmaId(Long alunoId, Long turmaId);
    long countByTurmaId(Long turmaId);
}
