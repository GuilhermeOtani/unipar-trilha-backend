package br.unipar.trilha.services;

import br.unipar.trilha.dtos.ProfessorContextoResponse;
import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.entities.VinculoProfessor;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.repositories.VinculoProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessorContextoService {
    private final UsuarioAutenticadoService autenticadoService;
    private final VinculoProfessorRepository vinculoRepository;

    @Transactional(readOnly = true)
    public ProfessorContextoResponse obter() {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        var turmas = vinculoRepository.findByProfessorIdOrderByTurmaNome(professor.getId()).stream()
                .map(this::toResumo)
                .toList();
        return new ProfessorContextoResponse(professor.getId(), professor.getNome(), turmas);
    }

    private ProfessorContextoResponse.TurmaResumo toResumo(VinculoProfessor vinculo) {
        var turma = vinculo.getTurma();
        var disciplina = turma.getDisciplina();
        return new ProfessorContextoResponse.TurmaResumo(turma.getId(), turma.getNome(), turma.getPeriodo(),
                disciplina.getId(), disciplina.getNome(), disciplina.getCodigo());
    }
}
