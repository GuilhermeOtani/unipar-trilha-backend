package br.unipar.trilha.services;

import br.unipar.trilha.dtos.RespostaAlunoRequest;
import br.unipar.trilha.dtos.RespostaAlunoResponse;
import br.unipar.trilha.dtos.SessaoResponse;
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
public class AprendizagemService {
    private final DistribuicaoRepository distribuicaoRepository;
    private final MatriculaAlunoRepository matriculaRepository;
    private final SessaoAprendizagemRepository sessaoRepository;
    private final DesafioVersaoRepository desafioRepository;
    private final OpcaoDesafioVersaoRepository opcaoRepository;
    private final TentativaRepository tentativaRepository;
    private final UsuarioAutenticadoService autenticadoService;

    @Transactional
    public SessaoResponse iniciarOuRetomar(Long distribuicaoId) {
        Usuario aluno = autenticadoService.exigirPerfil(Perfil.ALUNO);
        Distribuicao distribuicao = buscarDistribuicaoDoAluno(distribuicaoId, aluno.getId());
        var existente = sessaoRepository.findByDistribuicaoIdAndAlunoId(distribuicaoId, aluno.getId());
        if (existente.isPresent()) {
            SessaoAprendizagem sessao = existente.get();
            validarDistribuicaoAtivaParaSessaoIncompleta(sessao);
            return toResponse(sessao);
        }
        validarNovaSessao(distribuicao);
        SessaoAprendizagem sessao = criarSessao(distribuicao, aluno);
        return toResponse(sessao);
    }

    @Transactional(readOnly = true)
    public SessaoResponse buscar(Long sessaoId) {
        Usuario aluno = autenticadoService.exigirPerfil(Perfil.ALUNO);
        SessaoAprendizagem sessao = sessaoRepository.findByIdAndAlunoId(sessaoId, aluno.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada."));
        validarDistribuicaoAtivaParaSessaoIncompleta(sessao);
        return toResponse(sessao);
    }

    @Transactional
    public RespostaAlunoResponse responder(Long sessaoId, RespostaAlunoRequest request) {
        Usuario aluno = autenticadoService.exigirPerfil(Perfil.ALUNO);
        SessaoAprendizagem sessao = sessaoRepository.findByIdAndAlunoId(sessaoId, aluno.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada."));
        validarDistribuicaoAtivaParaSessaoIncompleta(sessao);
        if (sessao.getStatus() == StatusSessao.CONCLUIDA) {
            throw new RegraNegocioException("A sessão já foi concluída.");
        }
        DesafioVersao atual = sessao.getDesafioAtual();
        if (atual == null || !atual.getId().equals(request.desafioId())) {
            throw new RegraNegocioException("O desafio informado não é o desafio atual da sessão.");
        }
        OpcaoDesafioVersao opcao = opcaoRepository.findById(request.opcaoId())
                .filter(item -> item.getDesafio().getId().equals(atual.getId()))
                .orElseThrow(() -> new RegraNegocioException("A opção não pertence ao desafio atual."));
        boolean correta = Boolean.TRUE.equals(opcao.getCorreta());
        tentativaRepository.saveAndFlush(Tentativa.builder()
                .sessao(sessao)
                .desafio(atual)
                .opcao(opcao)
                .correta(correta)
                .respondidaEm(LocalDateTime.now())
                .build());

        DesafioVersao proximo = atual;
        if (correta) {
            List<DesafioVersao> desafios = desafios(sessao);
            int indice = indiceDoAtual(desafios, atual.getId());
            if (indice == desafios.size() - 1) {
                sessao.setStatus(StatusSessao.CONCLUIDA);
                sessao.setConcluidaEm(LocalDateTime.now());
                sessao.setDesafioAtual(null);
                proximo = null;
            } else {
                proximo = desafios.get(indice + 1);
                sessao.setDesafioAtual(proximo);
            }
            sessaoRepository.save(sessao);
        }
        SessaoResponse.ProgressoResponse progresso = progresso(sessao);
        String feedback = correta ? "Correto. " + atual.getExplicacao() : "Ainda não. " + atual.getExplicacao();
        return new RespostaAlunoResponse(correta, feedback, progresso, toDesafio(proximo));
    }

    private SessaoAprendizagem criarSessao(Distribuicao distribuicao, Usuario aluno) {
        List<DesafioVersao> desafios = desafioRepository.listarOrdenados(distribuicao.getVersao().getId());
        if (desafios.isEmpty()) {
            throw new RegraNegocioException("A versão distribuída não possui desafios.");
        }
        return sessaoRepository.save(SessaoAprendizagem.builder()
                .distribuicao(distribuicao)
                .aluno(aluno)
                .desafioAtual(desafios.getFirst())
                .status(StatusSessao.EM_ANDAMENTO)
                .build());
    }

    private Distribuicao buscarDistribuicaoDoAluno(Long id, Long alunoId) {
        Distribuicao distribuicao = distribuicaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Distribuição não encontrada."));
        if (!matriculaRepository.existsByAlunoIdAndTurmaId(alunoId, distribuicao.getTurma().getId())) {
            throw new RecursoNaoEncontradoException("Distribuição não encontrada para o aluno.");
        }
        return distribuicao;
    }

    private void validarNovaSessao(Distribuicao distribuicao) {
        if (!Boolean.TRUE.equals(distribuicao.getAtivo())) {
            throw new RegraNegocioException("A distribuição foi desativada e não aceita novas sessões.");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (distribuicao.getDisponivelDe().isAfter(agora)
                || (distribuicao.getDisponivelAte() != null && !distribuicao.getDisponivelAte().isAfter(agora))) {
            throw new RegraNegocioException("O prazo para iniciar esta distribuição está encerrado.");
        }
    }

    private void validarDistribuicaoAtivaParaSessaoIncompleta(SessaoAprendizagem sessao) {
        if (!Boolean.TRUE.equals(sessao.getDistribuicao().getAtivo())
                && sessao.getStatus() != StatusSessao.CONCLUIDA) {
            throw new RegraNegocioException("A distribuição foi desativada e esta sessão não pode continuar.");
        }
    }

    private SessaoResponse toResponse(SessaoAprendizagem sessao) {
        DesafioVersao desafio = sessao.getDesafioAtual();
        String licao = desafio == null ? null : desafio.getLicaoVersao().getTitulo();
        return new SessaoResponse(sessao.getId(), sessao.getDistribuicao().getId(),
                sessao.getDistribuicao().getVersao().getTitulo(),
                sessao.getDistribuicao().getVersao().getNumeroVersao(), licao,
                sessao.getStatus(), progresso(sessao), toDesafio(desafio));
    }

    private SessaoResponse.ProgressoResponse progresso(SessaoAprendizagem sessao) {
        int total = desafios(sessao).size();
        long respondidos = tentativaRepository.contarDesafiosAcertados(sessao.getId());
        int percentual = total == 0 ? 0 : (int) Math.round(respondidos * 100.0 / total);
        return new SessaoResponse.ProgressoResponse((int) respondidos, total, percentual,
                sessao.getStatus() == StatusSessao.CONCLUIDA);
    }

    private List<DesafioVersao> desafios(SessaoAprendizagem sessao) {
        return desafioRepository.listarOrdenados(sessao.getDistribuicao().getVersao().getId());
    }

    private int indiceDoAtual(List<DesafioVersao> desafios, Long atualId) {
        for (int i = 0; i < desafios.size(); i++) {
            if (desafios.get(i).getId().equals(atualId)) {
                return i;
            }
        }
        throw new RegraNegocioException("Desafio atual não pertence à versão distribuída.");
    }

    private SessaoResponse.DesafioAlunoResponse toDesafio(DesafioVersao desafio) {
        if (desafio == null) {
            return null;
        }
        var opcoes = desafio.getOpcoes().stream()
                .map(item -> new SessaoResponse.OpcaoAlunoResponse(item.getId(), item.getTexto()))
                .toList();
        return new SessaoResponse.DesafioAlunoResponse(desafio.getId(), desafio.getEnunciado(),
                desafio.getTipo(), desafio.getDificuldade(), opcoes);
    }
}
