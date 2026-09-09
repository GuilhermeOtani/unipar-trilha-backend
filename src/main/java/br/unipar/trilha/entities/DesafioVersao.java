package br.unipar.trilha.entities;

import br.unipar.trilha.enums.Dificuldade;
import br.unipar.trilha.enums.TipoDesafio;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "desafio_versao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesafioVersao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "licao_versao_id", nullable = false)
    private LicaoVersao licaoVersao;

    @Column(nullable = false, length = 2000)
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDesafio tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Dificuldade dificuldade;

    @Column(nullable = false, length = 2000)
    private String explicacao;

    @Column(nullable = false)
    private Integer ordem;

    @OneToMany(mappedBy = "desafio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<OpcaoDesafioVersao> opcoes = new ArrayList<>();

    public void adicionarOpcao(OpcaoDesafioVersao opcao) {
        opcao.setDesafio(this);
        opcoes.add(opcao);
    }
}
