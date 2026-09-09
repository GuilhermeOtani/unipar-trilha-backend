package br.unipar.trilha.dtos;

import java.time.LocalDateTime;

public record PublicacaoResponse(
        Long trilhaId,
        Long versaoId,
        Integer numeroVersao,
        LocalDateTime publicadaEm
) {
}
