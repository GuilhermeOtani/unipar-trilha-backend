package br.unipar.trilha.services;

import br.unipar.trilha.dtos.IndicadoresTurmaResponse;
import br.unipar.trilha.entities.Turma;
import br.unipar.trilha.entities.Usuario;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.enums.StatusSessao;
import br.unipar.trilha.exceptions.RecursoNaoEncontradoException;
import br.unipar.trilha.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AcompanhamentoService {
    private final TurmaRepository turmaRepository;
    private final VinculoProfessorRepository vinculoRepository;
    private final MatriculaAlunoRepository matriculaRepository;
    private final SessaoAprendizagemRepository sessaoRepository;
    private final TentativaRepository tentativaRepository;
    private final UsuarioAutenticadoService autenticadoService;

    @Transactional(readOnly = true)
    public IndicadoresTurmaResponse obter(Long turmaId) {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        if (!vinculoRepository.existsByProfessorIdAndTurmaId(professor.getId(), turmaId)) {
            throw new AccessDeniedException("Professor não está vinculado à turma.");
        }
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
        long matriculados = matriculaRepository.countByTurmaId(turmaId);
        long iniciaram = sessaoRepository.contarAlunosQueIniciaram(turmaId);
        long concluiram = sessaoRepository.contarAlunosPorStatus(turmaId, StatusSessao.CONCLUIDA);
        long tentativas = tentativaRepository.contarTentativas(turmaId);
        long acertos = tentativaRepository.contarAcertos(turmaId);
        long erros = tentativas - acertos;
        int percentualConclusao = percentual(concluiram, matriculados);
        int acuracia = percentual(acertos, tentativas);
        var dificuldades = tentativaRepository.agregarDificuldades(turmaId).stream()
                .map(this::toDificuldade)
                .sorted(Comparator.comparingInt(IndicadoresTurmaResponse.DesafioDificilResponse::taxaErro)
                        .reversed()
                        .thenComparing(Comparator.comparingLong(
                                IndicadoresTurmaResponse.DesafioDificilResponse::tentativas).reversed()))
                .toList();
        return new IndicadoresTurmaResponse(turmaId, turma.getNome(), matriculados, iniciaram, concluiram,
                percentualConclusao, acertos, erros, acuracia, dificuldades);
    }

    private IndicadoresTurmaResponse.DesafioDificilResponse toDificuldade(Object[] linha) {
        long tentativas = ((Number) linha[3]).longValue();
        long erros = ((Number) linha[4]).longValue();
        return new IndicadoresTurmaResponse.DesafioDificilResponse(
                ((Number) linha[0]).longValue(), (String) linha[1], ((Number) linha[2]).intValue(),
                tentativas, erros, percentual(erros, tentativas));
    }

    private int percentual(long parte, long total) {
        return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }
}
