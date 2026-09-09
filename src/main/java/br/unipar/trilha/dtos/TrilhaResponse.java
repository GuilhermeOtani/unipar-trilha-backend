package br.unipar.trilha.dtos;

import br.unipar.trilha.enums.Dificuldade;
import br.unipar.trilha.enums.StatusTrilha;
import br.unipar.trilha.enums.TipoDesafio;

import java.time.LocalDateTime;
import java.util.List;

public record TrilhaResponse(
        Long id,
        String titulo,
        String descricao,
        StatusTrilha status,
        Long disciplinaId,
        String disciplinaNome,
        Long professorId,
        List<ModuloResponse> modulos,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public record ModuloResponse(Long id, String titulo, Integer ordem, List<LicaoResponse> licoes) {
    }

    public record LicaoResponse(Long id, String titulo, String resumo, Integer ordem,
                                List<DesafioResponse> desafios) {
    }

    public record DesafioResponse(Long id, String enunciado, TipoDesafio tipo,
                                  Dificuldade dificuldade, String explicacao, Integer ordem,
                                  List<OpcaoResponse> opcoes) {
    }

    public record OpcaoResponse(Long id, String texto, Integer ordem, Boolean correta) {
    }
}
