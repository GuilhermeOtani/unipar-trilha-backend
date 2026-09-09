package br.unipar.trilha.controllers;

import br.unipar.trilha.dtos.UsuarioCreateRequest;
import br.unipar.trilha.dtos.UsuarioResponse;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.services.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuários")
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public UsuarioResponse atual() {
        return usuarioService.atual();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest request) {
        return usuarioService.criar(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<UsuarioResponse> listar(@RequestParam Perfil perfil) {
        return usuarioService.listar(perfil);
    }
}
