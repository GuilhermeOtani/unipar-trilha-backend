package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.StatusLicaoAluno;

import java.util.List;

public record CaminhoAlunoResponse(
        Long distribuicaoId,
        Long sessaoId,
        Long versaoId,
        Integer numeroVersao,
        String trilhaTitulo,
        String disciplinaNome,
        int percentualProgresso,
        List<ModuloResponse> modulos
) {
    public record ModuloResponse(Long id, String titulo, Integer ordem, List<LicaoResponse> licoes) {
    }

    public record LicaoResponse(
            Long id,
            String titulo,
            String resumo,
            Integer ordem,
            int totalDesafios,
            int desafiosConcluidos,
            StatusLicaoAluno status
    ) {
    }
}
