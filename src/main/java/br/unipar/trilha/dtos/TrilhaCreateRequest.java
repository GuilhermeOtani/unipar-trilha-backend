package br.unipar.trilha.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrilhaCreateRequest(
        @NotBlank(message = "título é obrigatório") @Size(max = 160) String titulo,
        @Size(max = 1000) String descricao,
        @NotNull(message = "disciplinaId é obrigatório") Long disciplinaId
) {
}
