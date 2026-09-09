package br.unipar.trilha.repositories;

import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.enums.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLogin(String login);
    Optional<Usuario> findByLoginAndAtivoTrue(String login);
    boolean existsByLogin(String login);
    List<Usuario> findByPerfilAndAtivoTrueOrderByNome(Perfil perfil);
}
