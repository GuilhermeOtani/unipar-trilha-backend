package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.Tentativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TentativaRepository extends JpaRepository<Tentativa, Long> {
    @Query("select count(distinct t.desafio.id) from Tentativa t where t.sessao.id = :sessaoId and t.correta = true")
    long contarDesafiosAcertados(@Param("sessaoId") Long sessaoId);

    @Query("select count(t) from Tentativa t where t.sessao.distribuicao.turma.id = :turmaId")
    long contarTentativas(@Param("turmaId") Long turmaId);

    @Query("select count(t) from Tentativa t where t.sessao.distribuicao.turma.id = :turmaId and t.correta = true")
    long contarAcertos(@Param("turmaId") Long turmaId);

    @Query("""
            select t.desafio.id, t.desafio.enunciado,
                   t.desafio.licaoVersao.moduloVersao.trilhaVersao.numeroVersao,
                   count(t), sum(case when t.correta = false then 1 else 0 end)
            from Tentativa t
            where t.sessao.distribuicao.turma.id = :turmaId
            group by t.desafio.id, t.desafio.enunciado,
                     t.desafio.licaoVersao.moduloVersao.trilhaVersao.numeroVersao
            """)
    List<Object[]> agregarDificuldades(@Param("turmaId") Long turmaId);
}
