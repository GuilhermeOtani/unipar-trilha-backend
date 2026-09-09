package br.unipar.trilha.dtos;

import java.util.List;

public record IndicadoresTurmaResponse(
        Long turmaId,
        String turmaNome,
        long matriculados,
        long iniciaram,
        long concluiram,
        int percentualConclusao,
        long acertos,
        long erros,
        int acuracia,
        List<DesafioDificilResponse> desafiosComMaisErros
) {
    public record DesafioDificilResponse(
            Long desafioId,
            String enunciado,
            Integer numeroVersao,
            long tentativas,
            long erros,
            int taxaErro
    ) {
    }
}
