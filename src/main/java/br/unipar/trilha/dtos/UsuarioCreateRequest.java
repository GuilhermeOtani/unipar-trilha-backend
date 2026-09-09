package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
        @NotBlank(message = "login é obrigatório") @Size(max = 60) String login,
        @NotBlank(message = "nome é obrigatório") @Size(max = 120) String nome,
        @NotBlank(message = "senha é obrigatória") @Size(min = 6, max = 72) String senha,
        @NotNull(message = "perfil é obrigatório") Perfil perfil
) {
}
