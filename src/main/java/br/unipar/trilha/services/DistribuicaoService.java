package br.unipar.trilha.services;

import br.unipar.trilha.dtos.CaminhoAlunoResponse;
import br.unipar.trilha.dtos.CatalogoAlunoResponse;
import br.unipar.trilha.dtos.DistribuicaoRequest;
import br.unipar.trilha.dtos.DistribuicaoResponse;
import br.unipar.trilha.entities.*;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.enums.StatusSessao;
import br.unipar.trilha.enums.StatusLicaoAluno;
import br.unipar.trilha.exceptions.RecursoNaoEncontradoException;
import br.unipar.trilha.exceptions.RegraNegocioException;
import br.unipar.trilha.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    public List<DistribuicaoResponse> listarParaProfessor(Long turmaId) {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        if (!vinculoRepository.existsByProfessorIdAndTurmaId(professor.getId(), turmaId)) {
            throw new AccessDeniedException("Professor não está vinculado à turma.");
        }
        return distribuicaoRepository
                .findByTurmaIdAndVersaoProfessorIdOrderByCriadoEmDesc(turmaId, professor.getId()).stream()
                .map(this::toResponse)
                .toList();
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
                .findByTurmaIdInOrderByDisponivelDeDesc(turmaIds).stream()
                .map(item -> new DistribuicaoComSessao(item,
                        sessaoRepository.findByDistribuicaoIdAndAlunoId(item.getId(), aluno.getId())))
                .filter(item -> visivelNoCatalogo(item.distribuicao(), item.sessao(), agora))
                .map(item -> toCatalogo(item.distribuicao(), item.sessao(), agora))
                .toList();
        return new CatalogoAlunoResponse(itens);
    }

    @Transactional(readOnly = true)
    public CaminhoAlunoResponse obterCaminho(Long distribuicaoId) {
        Usuario aluno = autenticadoService.exigirPerfil(Perfil.ALUNO);
        Distribuicao distribuicao = distribuicaoRepository.findById(distribuicaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Distribuição não encontrada."));
        if (!matriculaRepository.existsByAlunoIdAndTurmaId(aluno.getId(), distribuicao.getTurma().getId())) {
            throw new RecursoNaoEncontradoException("Distribuição não encontrada para o aluno.");
        }
        Optional<SessaoAprendizagem> sessao = sessaoRepository
                .findByDistribuicaoIdAndAlunoId(distribuicaoId, aluno.getId());
        if (!visivelNoCatalogo(distribuicao, sessao, LocalDateTime.now())) {
            throw new RecursoNaoEncontradoException("Distribuição não disponível para o aluno.");
        }

        Set<Long> desafiosConcluidos = sessao
                .map(item -> new HashSet<>(tentativaRepository.listarIdsDesafiosAcertados(item.getId())))
                .orElseGet(HashSet::new);
        List<CaminhoAlunoResponse.ModuloResponse> modulos = new ArrayList<>();
        boolean atualDefinida = false;
        for (ModuloVersao modulo : distribuicao.getVersao().getModulos()) {
            List<CaminhoAlunoResponse.LicaoResponse> licoes = new ArrayList<>();
            for (LicaoVersao licao : modulo.getLicoes()) {
                int total = licao.getDesafios().size();
                int concluidos = (int) licao.getDesafios().stream()
                        .filter(desafio -> desafiosConcluidos.contains(desafio.getId()))
                        .count();
                StatusLicaoAluno status;
                if (total > 0 && concluidos == total) {
                    status = StatusLicaoAluno.CONCLUIDA;
                } else if (!atualDefinida) {
                    status = StatusLicaoAluno.ATUAL;
                    atualDefinida = true;
                } else {
                    status = StatusLicaoAluno.BLOQUEADA;
                }
                licoes.add(new CaminhoAlunoResponse.LicaoResponse(licao.getId(), licao.getTitulo(),
                        licao.getResumo(), licao.getOrdem(), total, concluidos, status));
            }
            modulos.add(new CaminhoAlunoResponse.ModuloResponse(
                    modulo.getId(), modulo.getTitulo(), modulo.getOrdem(), licoes));
        }
        int total = desafioRepository.listarOrdenados(distribuicao.getVersao().getId()).size();
        int percentual = total == 0 ? 0 : (int) Math.round(desafiosConcluidos.size() * 100.0 / total);
        return new CaminhoAlunoResponse(distribuicao.getId(), sessao.map(SessaoAprendizagem::getId).orElse(null),
                distribuicao.getVersao().getId(), distribuicao.getVersao().getNumeroVersao(),
                distribuicao.getVersao().getTitulo(), distribuicao.getVersao().getDisciplina().getNome(),
                percentual, modulos);
    }

    private boolean visivelNoCatalogo(Distribuicao distribuicao, Optional<SessaoAprendizagem> sessao,
                                      LocalDateTime agora) {
        if (distribuicao.getDisponivelDe().isAfter(agora) && sessao.isEmpty()) {
            return false;
        }
        if (!Boolean.TRUE.equals(distribuicao.getAtivo())) {
            return sessao.map(item -> item.getStatus() == StatusSessao.CONCLUIDA).orElse(false);
        }
        boolean prazoEncerrado = distribuicao.getDisponivelAte() != null
                && !distribuicao.getDisponivelAte().isAfter(agora);
        return !prazoEncerrado || sessao.isPresent();
    }

    private CatalogoAlunoResponse.Item toCatalogo(Distribuicao distribuicao,
                                                   Optional<SessaoAprendizagem> sessao,
                                                   LocalDateTime agora) {
        int total = desafioRepository.listarOrdenados(distribuicao.getVersao().getId()).size();
        long respondidos = sessao.map(item -> tentativaRepository.contarDesafiosAcertados(item.getId())).orElse(0L);
        int percentual = total == 0 ? 0 : (int) Math.round(respondidos * 100.0 / total);
        boolean concluida = sessao.map(item -> item.getStatus() == StatusSessao.CONCLUIDA).orElse(false);
        boolean prazoEncerrado = distribuicao.getDisponivelAte() != null
                && !distribuicao.getDisponivelAte().isAfter(agora);
        return new CatalogoAlunoResponse.Item(distribuicao.getId(), distribuicao.getVersao().getTitulo(),
                distribuicao.getVersao().getDisciplina().getNome(), distribuicao.getVersao().getNumeroVersao(),
                total, percentual, concluida, sessao.map(SessaoAprendizagem::getId).orElse(null),
                distribuicao.getDisponivelDe(), distribuicao.getDisponivelAte(), prazoEncerrado);
    }

    private record DistribuicaoComSessao(Distribuicao distribuicao, Optional<SessaoAprendizagem> sessao) {
    }

    private DistribuicaoResponse toResponse(Distribuicao distribuicao) {
        return new DistribuicaoResponse(distribuicao.getId(), distribuicao.getVersao().getId(),
                distribuicao.getVersao().getNumeroVersao(), distribuicao.getVersao().getTitulo(),
                distribuicao.getTurma().getId(), distribuicao.getTurma().getNome(),
                distribuicao.getDisponivelDe(), distribuicao.getDisponivelAte());
    }
}
