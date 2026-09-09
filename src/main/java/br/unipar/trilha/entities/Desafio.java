package br.unipar.trilha.entities;

import br.unipar.trilha.enums.Dificuldade;
import br.unipar.trilha.enums.TipoDesafio;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "desafio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Desafio extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "licao_id", nullable = false)
    private Licao licao;

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
    private List<OpcaoDesafio> opcoes = new ArrayList<>();

    public void adicionarOpcao(OpcaoDesafio opcao) {
        opcao.setDesafio(this);
        opcoes.add(opcao);
    }
}
