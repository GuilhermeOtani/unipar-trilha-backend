package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.SessaoAprendizagem;
import br.unipar.trilha.enums.StatusSessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SessaoAprendizagemRepository extends JpaRepository<SessaoAprendizagem, Long> {
    Optional<SessaoAprendizagem> findByDistribuicaoIdAndAlunoId(Long distribuicaoId, Long alunoId);
    Optional<SessaoAprendizagem> findByIdAndAlunoId(Long id, Long alunoId);

    @Query("select count(distinct s.aluno.id) from SessaoAprendizagem s where s.distribuicao.turma.id = :turmaId")
    long contarAlunosQueIniciaram(@Param("turmaId") Long turmaId);

    @Query("select count(distinct s.aluno.id) from SessaoAprendizagem s where s.distribuicao.turma.id = :turmaId and s.status = :status")
    long contarAlunosPorStatus(@Param("turmaId") Long turmaId, @Param("status") StatusSessao status);
}
