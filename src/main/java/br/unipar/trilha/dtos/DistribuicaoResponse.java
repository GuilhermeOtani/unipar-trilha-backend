package br.unipar.trilha.dtos;

import java.time.LocalDateTime;

public record DistribuicaoResponse(
        Long id,
        Long versaoId,
        Integer numeroVersao,
        String trilhaTitulo,
        Long turmaId,
        String turmaNome,
        LocalDateTime disponivelDe,
        LocalDateTime disponivelAte
) {
}
