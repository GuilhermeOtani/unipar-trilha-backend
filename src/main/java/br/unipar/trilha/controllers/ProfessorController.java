package br.unipar.trilha.controllers;

import br.unipar.trilha.dtos.ProfessorContextoResponse;
import br.unipar.trilha.services.ProfessorContextoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/professor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Professor")
public class ProfessorController {
    private final ProfessorContextoService contextoService;

    @GetMapping("/contexto")
    public ProfessorContextoResponse contexto() {
        return contextoService.obter();
    }
}
