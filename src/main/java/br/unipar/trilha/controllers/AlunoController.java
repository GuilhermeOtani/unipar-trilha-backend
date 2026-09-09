package br.unipar.trilha.controllers;

import br.unipar.trilha.dtos.*;
import br.unipar.trilha.services.AprendizagemService;
import br.unipar.trilha.services.DistribuicaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/aluno")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ALUNO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Aluno")
public class AlunoController {
    private final DistribuicaoService distribuicaoService;
    private final AprendizagemService aprendizagemService;

    @GetMapping("/distribuicoes")
    public CatalogoAlunoResponse distribuicoes() {
        return distribuicaoService.listarParaAluno();
    }

    @PostMapping("/distribuicoes/{id}/sessoes")
    public SessaoResponse iniciarOuRetomar(@PathVariable Long id) {
        return aprendizagemService.iniciarOuRetomar(id);
    }

    @GetMapping("/sessoes/{id}")
    public SessaoResponse sessao(@PathVariable Long id) {
        return aprendizagemService.buscar(id);
    }

    @PostMapping("/sessoes/{id}/respostas")
    public RespostaAlunoResponse responder(@PathVariable Long id,
                                           @Valid @RequestBody RespostaAlunoRequest request) {
        return aprendizagemService.responder(id, request);
    }
}
