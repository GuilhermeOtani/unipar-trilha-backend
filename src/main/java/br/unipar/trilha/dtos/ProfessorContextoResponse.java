package br.unipar.trilha.dtos;

import java.util.List;

public record ProfessorContextoResponse(
        Long professorId,
        String professorNome,
        List<TurmaResumo> turmas
) {
    public record TurmaResumo(
            Long turmaId,
            String turmaNome,
            String periodo,
            Long disciplinaId,
            String disciplinaNome,
            String disciplinaCodigo
    ) {
    }
}
