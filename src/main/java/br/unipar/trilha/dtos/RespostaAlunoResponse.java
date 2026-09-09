package br.unipar.trilha.dtos;

public record RespostaAlunoResponse(
        boolean correta,
        String feedback,
        SessaoResponse.ProgressoResponse progresso,
        SessaoResponse.DesafioAlunoResponse proximoDesafio
) {
}
