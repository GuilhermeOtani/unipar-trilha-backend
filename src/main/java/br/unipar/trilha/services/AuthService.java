package br.unipar.trilha.services;

import br.unipar.trilha.dtos.LoginRequest;
import br.unipar.trilha.dtos.LoginResponse;
import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.repositories.UsuarioRepository;
import br.unipar.trilha.security.JwtProperties;
import br.unipar.trilha.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties properties;

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.login(), request.senha()));
        } catch (Exception ex) {
            throw new BadCredentialsException("Login ou senha inválidos.");
        }
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(request.login())
                .orElseThrow(() -> new BadCredentialsException("Login ou senha inválidos."));
        return new LoginResponse(tokenProvider.gerar(usuario), "Bearer", properties.expirationMinutes(),
                usuario.getId(), usuario.getLogin(), usuario.getNome(), usuario.getPerfil());
    }
}
