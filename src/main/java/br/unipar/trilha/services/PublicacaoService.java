package br.unipar.trilha.services;

import br.unipar.trilha.dtos.PublicacaoResponse;
import br.unipar.trilha.entities.*;
import br.unipar.trilha.enums.Perfil;
import br.unipar.trilha.exceptions.RecursoNaoEncontradoException;
import br.unipar.trilha.repositories.TrilhaRepository;
import br.unipar.trilha.repositories.TrilhaVersaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PublicacaoService {
    private final TrilhaRepository trilhaRepository;
    private final TrilhaVersaoRepository versaoRepository;
    private final UsuarioAutenticadoService autenticadoService;
    private final TrilhaService trilhaService;

    @Transactional
    public PublicacaoResponse publicar(Long trilhaId) {
        Usuario professor = autenticadoService.exigirPerfil(Perfil.PROFESSOR);
        Trilha trilha = trilhaRepository.buscarParaPublicacao(trilhaId, professor.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trilha não encontrada."));
        trilhaService.validarPublicavel(trilha);
        int numero = versaoRepository.maiorNumeroVersao(trilhaId) + 1;
        LocalDateTime agora = LocalDateTime.now();
        TrilhaVersao versao = TrilhaVersao.builder()
                .trilha(trilha)
                .numeroVersao(numero)
                .titulo(trilha.getTitulo())
                .descricao(trilha.getDescricao())
                .disciplina(trilha.getDisciplina())
                .professor(professor)
                .publicadaEm(agora)
                .build();
        trilha.getModulos().stream().map(this::copiar).forEach(versao::adicionarModulo);
        versao = versaoRepository.save(versao);
        return new PublicacaoResponse(trilhaId, versao.getId(), numero, agora);
    }

    private ModuloVersao copiar(Modulo origem) {
        ModuloVersao destino = ModuloVersao.builder().titulo(origem.getTitulo()).ordem(origem.getOrdem()).build();
        origem.getLicoes().stream().map(this::copiar).forEach(destino::adicionarLicao);
        return destino;
    }

    private LicaoVersao copiar(Licao origem) {
        LicaoVersao destino = LicaoVersao.builder().titulo(origem.getTitulo()).resumo(origem.getResumo())
                .ordem(origem.getOrdem()).build();
        origem.getDesafios().stream().map(this::copiar).forEach(destino::adicionarDesafio);
        return destino;
    }

    private DesafioVersao copiar(Desafio origem) {
        DesafioVersao destino = DesafioVersao.builder().enunciado(origem.getEnunciado()).tipo(origem.getTipo())
                .dificuldade(origem.getDificuldade()).explicacao(origem.getExplicacao())
                .ordem(origem.getOrdem()).build();
        origem.getOpcoes().stream().map(this::copiar).forEach(destino::adicionarOpcao);
        return destino;
    }

    private OpcaoDesafioVersao copiar(OpcaoDesafio origem) {
        return OpcaoDesafioVersao.builder().texto(origem.getTexto()).ordem(origem.getOrdem())
                .correta(origem.getCorreta()).build();
    }
}
