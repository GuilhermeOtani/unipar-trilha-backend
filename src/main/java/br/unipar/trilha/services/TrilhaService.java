package br.unipar.trilha.services;

import br.unipar.trilha.dtos.TrilhaConteudoRequest;
import br.unipar.trilha.dtos.TrilhaCreateRequest;
import br.unipar.trilha.dtos.TrilhaResponse;
import br.unipar.trilha.dtos.TrilhaResumoProfessorResponse;
import br.unipar.trilha.entities.*;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.enums.StatusTrilha;
import br.unipar.trilha.enums.TipoDesafio;
import br.unipar.trilha.exceptions.RecursoNaoEncontradoException;
import br.unipar.trilha.exceptions.RegraNegocioException;
import br.unipar.trilha.repositories.DisciplinaRepository;
import br.unipar.trilha.repositories.TrilhaRepository;
import br.unipar.trilha.repositories.TrilhaVersaoRepository;
import br.unipar.trilha.repositories.VinculoProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrilhaService {
    private final TrilhaRepository trilhaRepository;
    private final TrilhaVersaoRepository versaoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final VinculoProfessorRepository vinculoRepository;
    private final UsuarioAutenticadoService autenticadoService;

    @Transactional
    public TrilhaResponse criar(TrilhaCreateRequest request) {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        Disciplina disciplina = buscarDisciplinaPermitida(request.disciplinaId(), professor.getId());
        Trilha trilha = Trilha.builder()
                .titulo(request.titulo().trim())
                .descricao(normalizar(request.descricao()))
                .status(StatusTrilha.RASCUNHO)
                .disciplina(disciplina)
                .professor(professor)
                .build();
        return toResponse(trilhaRepository.save(trilha));
    }

    @Transactional(readOnly = true)
    public TrilhaResponse buscar(Long id) {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        return toResponse(buscarDoProfessor(id, professor.getId()));
    }

    @Transactional(readOnly = true)
    public List<TrilhaResumoProfessorResponse> listar() {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        return trilhaRepository.findByProfessorIdOrderByAtualizadoEmDesc(professor.getId()).stream()
                .map(this::toResumo)
                .toList();
    }

    @Transactional
    public TrilhaResponse atualizar(Long id, TrilhaConteudoRequest request) {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        Trilha trilha = buscarDoProfessor(id, professor.getId());
        if (trilha.getStatus() == StatusTrilha.ARQUIVADA) {
            throw new RegraNegocioException("Uma trilha arquivada não pode ser editada.");
        }
        validarConteudo(request);
        trilha.setDisciplina(buscarDisciplinaPermitida(request.disciplinaId(), professor.getId()));
        trilha.setTitulo(request.titulo().trim());
        trilha.setDescricao(normalizar(request.descricao()));
        trilha.getModulos().clear();
        trilhaRepository.flush();
        request.modulos().stream().map(this::toEntity).forEach(trilha::adicionarModulo);
        return toResponse(trilhaRepository.save(trilha));
    }

    public void validarPublicavel(Trilha trilha) {
        if (trilha.getModulos().isEmpty()
                || trilha.getModulos().stream().anyMatch(modulo -> modulo.getLicoes().isEmpty())
                || trilha.getModulos().stream().flatMap(modulo -> modulo.getLicoes().stream())
                .anyMatch(licao -> licao.getDesafios().isEmpty())) {
            throw new RegraNegocioException("A trilha precisa ter módulo, lição e desafio antes da publicação.");
        }
        trilha.getModulos().stream().flatMap(modulo -> modulo.getLicoes().stream())
                .flatMap(licao -> licao.getDesafios().stream())
                .forEach(desafio -> {
                    long corretas = desafio.getOpcoes().stream().filter(opcao -> Boolean.TRUE.equals(opcao.getCorreta())).count();
                    if (desafio.getOpcoes().size() < 2 || corretas != 1) {
                        throw new RegraNegocioException("Cada desafio deve possuir duas opções e exatamente uma correta.");
                    }
                });
    }

    private Disciplina buscarDisciplinaPermitida(Long disciplinaId, Long professorId) {
        Disciplina disciplina = disciplinaRepository.findById(disciplinaId)
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada."));
        boolean vinculado = vinculoRepository.findByProfessorIdOrderByTurmaNome(professorId).stream()
                .anyMatch(vinculo -> vinculo.getTurma().getDisciplina().getId().equals(disciplinaId));
        if (!vinculado) {
            throw new RegraNegocioException("Professor não está vinculado a esta disciplina.");
        }
        return disciplina;
    }

    private Trilha buscarDoProfessor(Long id, Long professorId) {
        return trilhaRepository.findByIdAndProfessorId(id, professorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trilha não encontrada."));
    }

    private void validarConteudo(TrilhaConteudoRequest request) {
        validarOrdens(request.modulos().stream().map(TrilhaConteudoRequest.ModuloRequest::ordem).toList(), "módulos");
        request.modulos().forEach(modulo -> {
            validarOrdens(modulo.licoes().stream().map(TrilhaConteudoRequest.LicaoRequest::ordem).toList(), "lições");
            modulo.licoes().forEach(licao -> {
                validarOrdens(licao.desafios().stream().map(TrilhaConteudoRequest.DesafioRequest::ordem).toList(), "desafios");
                licao.desafios().forEach(desafio -> {
                    if (desafio.tipo() != TipoDesafio.MULTIPLA_ESCOLHA) {
                        throw new RegraNegocioException("Somente múltipla escolha é suportada no MVP.");
                    }
                    validarOrdens(desafio.opcoes().stream().map(TrilhaConteudoRequest.OpcaoRequest::ordem).toList(), "opções");
                    if (desafio.opcoes().stream().filter(opcao -> Boolean.TRUE.equals(opcao.correta())).count() != 1) {
                        throw new RegraNegocioException("Cada desafio deve possuir exatamente uma opção correta.");
                    }
                });
            });
        });
    }

    private void validarOrdens(List<Integer> ordens, String nivel) {
        Set<Integer> unicas = new HashSet<>(ordens);
        if (unicas.size() != ordens.size()) {
            throw new RegraNegocioException("Existem ordens repetidas em " + nivel + ".");
        }
    }

    private Modulo toEntity(TrilhaConteudoRequest.ModuloRequest request) {
        Modulo modulo = Modulo.builder().titulo(request.titulo().trim()).ordem(request.ordem()).build();
        request.licoes().stream().map(this::toEntity).forEach(modulo::adicionarLicao);
        return modulo;
    }

    private Licao toEntity(TrilhaConteudoRequest.LicaoRequest request) {
        Licao licao = Licao.builder().titulo(request.titulo().trim()).resumo(normalizar(request.resumo()))
                .ordem(request.ordem()).build();
        request.desafios().stream().map(this::toEntity).forEach(licao::adicionarDesafio);
        return licao;
    }

    private Desafio toEntity(TrilhaConteudoRequest.DesafioRequest request) {
        Desafio desafio = Desafio.builder().enunciado(request.enunciado().trim()).tipo(request.tipo())
                .dificuldade(request.dificuldade()).explicacao(request.explicacao().trim())
                .ordem(request.ordem()).build();
        request.opcoes().stream().map(this::toEntity).forEach(desafio::adicionarOpcao);
        return desafio;
    }

    private OpcaoDesafio toEntity(TrilhaConteudoRequest.OpcaoRequest request) {
        return OpcaoDesafio.builder().texto(request.texto().trim()).ordem(request.ordem())
                .correta(request.correta()).build();
    }

    private TrilhaResponse toResponse(Trilha trilha) {
        return new TrilhaResponse(trilha.getId(), trilha.getTitulo(), trilha.getDescricao(), trilha.getStatus(),
                trilha.getDisciplina().getId(), trilha.getDisciplina().getNome(), trilha.getProfessor().getId(),
                trilha.getModulos().stream().map(this::toResponse).toList(),
                trilha.getCriadoEm(), trilha.getAtualizadoEm());
    }

    private TrilhaResumoProfessorResponse toResumo(Trilha trilha) {
        var publicacoes = versaoRepository.findByTrilhaIdOrderByNumeroVersaoDesc(trilha.getId()).stream()
                .map(versao -> new TrilhaResumoProfessorResponse.PublicacaoResumo(
                        versao.getId(), versao.getNumeroVersao(), versao.getPublicadaEm()))
                .toList();
        return new TrilhaResumoProfessorResponse(trilha.getId(), trilha.getTitulo(), trilha.getDescricao(),
                trilha.getStatus(), trilha.getDisciplina().getId(), trilha.getDisciplina().getNome(),
                trilha.getAtualizadoEm(), publicacoes);
    }

    private TrilhaResponse.ModuloResponse toResponse(Modulo modulo) {
        return new TrilhaResponse.ModuloResponse(modulo.getId(), modulo.getTitulo(), modulo.getOrdem(),
                modulo.getLicoes().stream().map(this::toResponse).toList());
    }

    private TrilhaResponse.LicaoResponse toResponse(Licao licao) {
        return new TrilhaResponse.LicaoResponse(licao.getId(), licao.getTitulo(), licao.getResumo(), licao.getOrdem(),
                licao.getDesafios().stream().map(this::toResponse).toList());
    }

    private TrilhaResponse.DesafioResponse toResponse(Desafio desafio) {
        return new TrilhaResponse.DesafioResponse(desafio.getId(), desafio.getEnunciado(), desafio.getTipo(),
                desafio.getDificuldade(), desafio.getExplicacao(), desafio.getOrdem(),
                desafio.getOpcoes().stream().map(opcao -> new TrilhaResponse.OpcaoResponse(
                        opcao.getId(), opcao.getTexto(), opcao.getOrdem(), opcao.getCorreta())).toList());
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
