package br.unipar.trilha.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "login é obrigatório") String login,
        @NotBlank(message = "senha é obrigatória") String senha
) {
}
