package br.unipar.trilha.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tentativa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tentativa extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoAprendizagem sessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desafio_id", nullable = false)
    private DesafioVersao desafio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false)
    private OpcaoDesafioVersao opcao;

    @Column(nullable = false)
    private Boolean correta;

    @Column(name = "respondida_em", nullable = false)
    private LocalDateTime respondidaEm;
}
