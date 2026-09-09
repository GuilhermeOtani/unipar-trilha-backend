package br.unipar.trilha.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "licao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Licao extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 1000)
    private String resumo;

    @Column(nullable = false)
    private Integer ordem;

    @OneToMany(mappedBy = "licao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<Desafio> desafios = new ArrayList<>();

    public void adicionarDesafio(Desafio desafio) {
        desafio.setLicao(this);
        desafios.add(desafio);
    }
}
