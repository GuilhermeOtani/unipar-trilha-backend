package br.unipar.trilha.services;

import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.exceptions.RecursoNaoEncontradoException;
import br.unipar.trilha.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioAutenticadoService {
    private final UsuarioRepository usuarioRepository;

    public Usuario obter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        return usuarioRepository.findByLoginAndAtivoTrue(authentication.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário autenticado não foi encontrado."));
    }

    public Usuario exigirPerfil(Perfil perfil) {
        Usuario usuario = obter();
        if (usuario.getPerfil() != perfil) {
            throw new AccessDeniedException("Perfil necessário: " + perfil.name());
        }
        return usuario;
    }
}
