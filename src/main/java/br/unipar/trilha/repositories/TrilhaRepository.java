package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.Trilha;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrilhaRepository extends JpaRepository<Trilha, Long> {
    Optional<Trilha> findByIdAndProfessorId(Long id, Long professorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trilha t where t.id = :id and t.professor.id = :professorId")
    Optional<Trilha> buscarParaPublicacao(@Param("id") Long id, @Param("professorId") Long professorId);
}
