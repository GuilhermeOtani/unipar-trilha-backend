package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.StatusTrilha;

import java.time.LocalDateTime;
import java.util.List;

public record TrilhaResumoProfessorResponse(
        Long id,
        String titulo,
        String descricao,
        StatusTrilha status,
        Long disciplinaId,
        String disciplinaNome,
        LocalDateTime atualizadoEm,
        List<PublicacaoResumo> publicacoes
) {
    public record PublicacaoResumo(Long versaoId, Integer numeroVersao, LocalDateTime publicadaEm) {
    }
}
