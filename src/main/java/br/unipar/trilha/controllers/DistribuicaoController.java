package br.unipar.trilha.controllers;

import br.unipar.trilha.dtos.DistribuicaoRequest;
import br.unipar.trilha.dtos.DistribuicaoResponse;
import br.unipar.trilha.services.DistribuicaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/distribuicoes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Distribuições")
public class DistribuicaoController {
    private final DistribuicaoService distribuicaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DistribuicaoResponse criar(@Valid @RequestBody DistribuicaoRequest request) {
        return distribuicaoService.criar(request);
    }
}
