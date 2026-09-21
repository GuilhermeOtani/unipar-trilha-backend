package br.unipar.trilha.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record CatalogoAlunoResponse(List<Item> distribuicoes) {
    public record Item(
            Long distribuicaoId,
            String trilhaTitulo,
            String disciplinaNome,
            Integer numeroVersao,
            int totalDesafios,
            int percentualProgresso,
            boolean concluida,
            Long sessaoId,
            LocalDateTime disponivelDe,
            LocalDateTime disponivelAte,
            boolean prazoEncerrado
    ) {
    }
}
