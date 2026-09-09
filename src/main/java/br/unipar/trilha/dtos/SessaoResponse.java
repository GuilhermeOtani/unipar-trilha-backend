package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.Dificuldade;
import br.unipar.trilha.enums.StatusSessao;
import br.unipar.trilha.enums.TipoDesafio;

import java.util.List;

public record SessaoResponse(
        Long sessaoId,
        Long distribuicaoId,
        String trilhaTitulo,
        Integer numeroVersao,
        String licaoTitulo,
        StatusSessao status,
        ProgressoResponse progresso,
        DesafioAlunoResponse desafioAtual
) {
    public record ProgressoResponse(int respondidos, int total, int percentual, boolean concluida) {
    }

    public record DesafioAlunoResponse(
            Long id,
            String enunciado,
            TipoDesafio tipo,
            Dificuldade dificuldade,
            List<OpcaoAlunoResponse> opcoes
    ) {
    }

    public record OpcaoAlunoResponse(Long id, String texto) {
    }
}
