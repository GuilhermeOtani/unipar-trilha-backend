package br.unipar.trilha.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "opcao_desafio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcaoDesafio extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desafio_id", nullable = false)
    private Desafio desafio;

    @Column(nullable = false, length = 1000)
    private String texto;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false)
    private Boolean correta;
}
