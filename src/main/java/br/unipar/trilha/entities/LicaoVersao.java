package br.unipar.trilha.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "licao_versao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicaoVersao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_versao_id", nullable = false)
    private ModuloVersao moduloVersao;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 1000)
    private String resumo;

    @Column(nullable = false)
    private Integer ordem;

    @OneToMany(mappedBy = "licaoVersao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<DesafioVersao> desafios = new ArrayList<>();

    public void adicionarDesafio(DesafioVersao desafio) {
        desafio.setLicaoVersao(this);
        desafios.add(desafio);
    }
}
