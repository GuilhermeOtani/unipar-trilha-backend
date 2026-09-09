package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.TrilhaVersao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrilhaVersaoRepository extends JpaRepository<TrilhaVersao, Long> {
    @Query("select coalesce(max(v.numeroVersao), 0) from TrilhaVersao v where v.trilha.id = :trilhaId")
    int maiorNumeroVersao(@Param("trilhaId") Long trilhaId);
}
