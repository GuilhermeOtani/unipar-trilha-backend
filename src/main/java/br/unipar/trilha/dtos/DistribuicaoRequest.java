package br.unipar.trilha.dtos;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DistribuicaoRequest(
        @NotNull(message = "versaoId é obrigatório") Long versaoId,
        @NotNull(message = "turmaId é obrigatório") Long turmaId,
        @NotNull(message = "disponivelDe é obrigatório") LocalDateTime disponivelDe,
        LocalDateTime disponivelAte
) {
}
