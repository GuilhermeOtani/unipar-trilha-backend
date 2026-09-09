package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.Dificuldade;
import br.unipar.trilha.enums.TipoDesafio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record TrilhaConteudoRequest(
        @NotBlank(message = "título é obrigatório") @Size(max = 160) String titulo,
        @Size(max = 1000) String descricao,
        @NotNull(message = "disciplinaId é obrigatório") Long disciplinaId,
        @NotEmpty(message = "a trilha deve possuir ao menos um módulo") List<@Valid ModuloRequest> modulos
) {
    public record ModuloRequest(
            @NotBlank(message = "título do módulo é obrigatório") @Size(max = 160) String titulo,
            @NotNull @Positive Integer ordem,
            @NotEmpty(message = "o módulo deve possuir ao menos uma lição") List<@Valid LicaoRequest> licoes
    ) {
    }

    public record LicaoRequest(
            @NotBlank(message = "título da lição é obrigatório") @Size(max = 160) String titulo,
            @Size(max = 1000) String resumo,
            @NotNull @Positive Integer ordem,
            @NotEmpty(message = "a lição deve possuir ao menos um desafio") List<@Valid DesafioRequest> desafios
    ) {
    }

    public record DesafioRequest(
            @NotBlank(message = "enunciado é obrigatório") @Size(max = 2000) String enunciado,
            @NotNull(message = "tipo é obrigatório") TipoDesafio tipo,
            @NotNull(message = "dificuldade é obrigatória") Dificuldade dificuldade,
            @NotBlank(message = "explicação é obrigatória") @Size(max = 2000) String explicacao,
            @NotNull @Positive Integer ordem,
            @Size(min = 2, message = "o desafio deve possuir ao menos duas opções")
            List<@Valid OpcaoRequest> opcoes
    ) {
    }

    public record OpcaoRequest(
            @NotBlank(message = "texto da opção é obrigatório") @Size(max = 1000) String texto,
            @NotNull @Positive Integer ordem,
            @NotNull(message = "correta é obrigatório") Boolean correta
    ) {
    }
}
