package br.unipar.trilha.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "opcao_desafio_versao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcaoDesafioVersao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desafio_versao_id", nullable = false)
    private DesafioVersao desafio;

    @Column(nullable = false, length = 1000)
    private String texto;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false)
    private Boolean correta;
}
