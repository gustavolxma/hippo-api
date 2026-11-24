package com.backend.hippo_api.infrastructure.entity;

import com.backend.hippo_api.infrastructure.enums.PrioridadeTarefaEnum;
import com.backend.hippo_api.infrastructure.enums.StatusTarefaEnum;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tarefas")
@Builder
public class Tarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_tarefa", nullable = false)
    private String nomeTarefa;
    @Column(name = "descricao", nullable = false, length = 2000)
    private String descricao;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private StatusTarefaEnum statusTarefa;
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;
    @Column(name = "data_evento")
    private LocalDateTime dataEvento;

    // false = a Tarefa tem só o dia. Nulo (Tarefas antigas) vale true.
    @Column(name = "tem_hora")
    private Boolean temHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", length = 30)
    private PrioridadeTarefaEnum prioridade;

    @Column(name = "lembrete_minutos_antes")
    private Integer lembreteMinutosAntes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
}
