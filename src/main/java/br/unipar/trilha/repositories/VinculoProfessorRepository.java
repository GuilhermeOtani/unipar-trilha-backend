package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.VinculoProfessor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VinculoProfessorRepository extends JpaRepository<VinculoProfessor, Long> {
    List<VinculoProfessor> findByProfessorIdOrderByTurmaNome(Long professorId);
    boolean existsByProfessorIdAndTurmaId(Long professorId, Long turmaId);
}
