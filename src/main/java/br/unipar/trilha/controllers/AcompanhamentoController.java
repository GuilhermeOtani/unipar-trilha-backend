package br.unipar.trilha.controllers;

import br.unipar.trilha.dtos.IndicadoresTurmaResponse;
import br.unipar.trilha.services.AcompanhamentoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/professor/turmas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Acompanhamento")
public class AcompanhamentoController {
    private final AcompanhamentoService acompanhamentoService;

    @GetMapping("/{id}/indicadores")
    public IndicadoresTurmaResponse indicadores(@PathVariable Long id) {
        return acompanhamentoService.obter(id);
    }
}
