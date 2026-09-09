package br.unipar.trilha.services;

import br.unipar.trilha.dtos.CatalogoAlunoResponse;
import br.unipar.trilha.dtos.DistribuicaoRequest;
import br.unipar.trilha.dtos.DistribuicaoResponse;
import br.unipar.trilha.entities.*;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.enums.StatusSessao;
import br.unipar.trilha.exceptions.RecursoNaoEncontradoException;
import br.unipar.trilha.exceptions.RegraNegocioException;
import br.unipar.trilha.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistribuicaoService {
    private final DistribuicaoRepository distribuicaoRepository;
    private final TrilhaVersaoRepository versaoRepository;
    private final TurmaRepository turmaRepository;
    private final VinculoProfessorRepository vinculoRepository;
    private final MatriculaAlunoRepository matriculaRepository;
    private final SessaoAprendizagemRepository sessaoRepository;
    private final TentativaRepository tentativaRepository;
    private final DesafioVersaoRepository desafioRepository;
    private final UsuarioAutenticadoService autenticadoService;

    @Transactional
    public DistribuicaoResponse criar(DistribuicaoRequest request) {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        if (request.disponivelAte() != null && !request.disponivelAte().isAfter(request.disponivelDe())) {
            throw new RegraNegocioException("disponivelAte deve ser posterior a disponivelDe.");
        }
        TrilhaVersao versao = versaoRepository.findById(request.versaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Versão publicada não encontrada."));
        Turma turma = turmaRepository.findById(request.turmaId())
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
        if (!versao.getProfessor().getId().equals(professor.getId())
                || !vinculoRepository.existsByProfessorIdAndTurmaId(professor.getId(), turma.getId())) {
            throw new RegraNegocioException("Professor não está autorizado a distribuir para esta turma.");
        }
        if (!versao.getDisciplina().getId().equals(turma.getDisciplina().getId())) {
            throw new RegraNegocioException("Versão e turma pertencem a disciplinas diferentes.");
        }
        if (distribuicaoRepository.existsByVersaoIdAndTurmaId(versao.getId(), turma.getId())) {
            throw new RegraNegocioException("Esta versão já foi distribuída para a turma.");
        }
        Distribuicao distribuicao = Distribuicao.builder()
                .versao(versao)
                .turma(turma)
                .disponivelDe(request.disponivelDe())
                .disponivelAte(request.disponivelAte())
                .ativo(true)
                .build();
        return toResponse(distribuicaoRepository.save(distribuicao));
    }

    @Transactional(readOnly = true)
    public CatalogoAlunoResponse listarParaAluno() {
        Usuario aluno = autenticadoService.exigirPerfil(Perfil.ALUNO);
        List<Long> turmaIds = matriculaRepository.findByAlunoId(aluno.getId()).stream()
                .map(matricula -> matricula.getTurma().getId()).toList();
        if (turmaIds.isEmpty()) {
            return new CatalogoAlunoResponse(List.of());
        }
        LocalDateTime agora = LocalDateTime.now();
        List<CatalogoAlunoResponse.Item> itens = distribuicaoRepository
                .findByTurmaIdInAndAtivoTrueOrderByDisponivelDeDesc(turmaIds).stream()
                .filter(item -> !item.getDisponivelDe().isAfter(agora))
                .filter(item -> item.getDisponivelAte() == null || item.getDisponivelAte().isAfter(agora))
                .map(item -> toCatalogo(item, aluno.getId()))
                .toList();
        return new CatalogoAlunoResponse(itens);
    }

    private CatalogoAlunoResponse.Item toCatalogo(Distribuicao distribuicao, Long alunoId) {
        int total = desafioRepository.listarOrdenados(distribuicao.getVersao().getId()).size();
        var sessao = sessaoRepository.findByDistribuicaoIdAndAlunoId(distribuicao.getId(), alunoId);
        long respondidos = sessao.map(item -> tentativaRepository.contarDesafiosAcertados(item.getId())).orElse(0L);
        int percentual = total == 0 ? 0 : (int) Math.round(respondidos * 100.0 / total);
        boolean concluida = sessao.map(item -> item.getStatus() == StatusSessao.CONCLUIDA).orElse(false);
        return new CatalogoAlunoResponse.Item(distribuicao.getId(), distribuicao.getVersao().getTitulo(),
                distribuicao.getVersao().getDisciplina().getNome(), distribuicao.getVersao().getNumeroVersao(),
                total, percentual, concluida);
    }

    private DistribuicaoResponse toResponse(Distribuicao distribuicao) {
        return new DistribuicaoResponse(distribuicao.getId(), distribuicao.getVersao().getId(),
                distribuicao.getVersao().getNumeroVersao(), distribuicao.getVersao().getTitulo(),
                distribuicao.getTurma().getId(), distribuicao.getTurma().getNome(),
                distribuicao.getDisponivelDe(), distribuicao.getDisponivelAte());
    }
}
