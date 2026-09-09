package br.unipar.trilha.services;

import br.unipar.trilha.dtos.UsuarioCreateRequest;
import br.unipar.trilha.dtos.UsuarioResponse;
import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.exceptions.RegraNegocioException;
import br.unipar.trilha.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAutenticadoService autenticadoService;

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest request) {
        if (usuarioRepository.existsByLogin(request.login())) {
            throw new RegraNegocioException("Já existe um usuário com este login.");
        }
        Usuario usuario = Usuario.builder()
                .login(request.login().trim())
                .nome(request.nome().trim())
                .senha(passwordEncoder.encode(request.senha()))
                .perfil(request.perfil())
                .ativo(true)
                .build();
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Perfil perfil) {
        return usuarioRepository.findByPerfilAndAtivoTrueOrderByNome(perfil).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse atual() {
        return toResponse(autenticadoService.obter());
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getLogin(), usuario.getNome(),
                usuario.getPerfil(), usuario.getAtivo());
    }
}
