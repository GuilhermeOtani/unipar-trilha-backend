package br.unipar.trilha.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modulo_versao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuloVersao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trilha_versao_id", nullable = false)
    private TrilhaVersao trilhaVersao;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false)
    private Integer ordem;

    @OneToMany(mappedBy = "moduloVersao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<LicaoVersao> licoes = new ArrayList<>();

    public void adicionarLicao(LicaoVersao licao) {
        licao.setModuloVersao(this);
        licoes.add(licao);
    }
}
