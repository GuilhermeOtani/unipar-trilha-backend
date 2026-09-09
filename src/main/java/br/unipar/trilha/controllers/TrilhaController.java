package br.unipar.trilha.controllers;

import br.unipar.trilha.dtos.*;
import br.unipar.trilha.services.PublicacaoService;
import br.unipar.trilha.services.TrilhaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trilhas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Trilhas")
public class TrilhaController {
    private final TrilhaService trilhaService;
    private final PublicacaoService publicacaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrilhaResponse criar(@Valid @RequestBody TrilhaCreateRequest request) {
        return trilhaService.criar(request);
    }

    @GetMapping("/{id}")
    public TrilhaResponse buscar(@PathVariable Long id) {
        return trilhaService.buscar(id);
    }

    @PutMapping("/{id}")
    public TrilhaResponse atualizar(@PathVariable Long id, @Valid @RequestBody TrilhaConteudoRequest request) {
        return trilhaService.atualizar(id, request);
    }

    @PostMapping("/{id}/publicacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacaoResponse publicar(@PathVariable Long id) {
        return publicacaoService.publicar(id);
    }
}
