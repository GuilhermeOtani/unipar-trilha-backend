package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.Distribuicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistribuicaoRepository extends JpaRepository<Distribuicao, Long> {
    boolean existsByVersaoIdAndTurmaId(Long versaoId, Long turmaId);
    List<Distribuicao> findByTurmaIdInAndAtivoTrueOrderByDisponivelDeDesc(List<Long> turmaIds);
    List<Distribuicao> findByTurmaIdInOrderByDisponivelDeDesc(List<Long> turmaIds);
    List<Distribuicao> findByTurmaIdAndAtivoTrue(Long turmaId);
    Optional<Distribuicao> findByIdAndAtivoTrue(Long id);
    List<Distribuicao> findByTurmaIdAndVersaoProfessorIdOrderByCriadoEmDesc(Long turmaId, Long professorId);
}
