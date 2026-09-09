package br.unipar.trilha.dtos;

import jakarta.validation.constraints.NotNull;

public record RespostaAlunoRequest(
        @NotNull(message = "desafioId é obrigatório") Long desafioId,
        @NotNull(message = "opcaoId é obrigatório") Long opcaoId
) {
}
