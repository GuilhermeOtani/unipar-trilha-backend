package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.DesafioVersao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DesafioVersaoRepository extends JpaRepository<DesafioVersao, Long> {
    @Query("""
            select d from DesafioVersao d
            join d.licaoVersao l
            join l.moduloVersao m
            where m.trilhaVersao.id = :versaoId
            order by m.ordem, l.ordem, d.ordem
            """)
    List<DesafioVersao> listarOrdenados(@Param("versaoId") Long versaoId);
}
