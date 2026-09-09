package br.unipar.trilha.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "distribuicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Distribuicao extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_id", nullable = false)
    private TrilhaVersao versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @Column(name = "disponivel_de", nullable = false)
    private LocalDateTime disponivelDe;

    @Column(name = "disponivel_ate")
    private LocalDateTime disponivelAte;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
