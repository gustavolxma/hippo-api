package com.backend.hippo_api.infrastructure.entity;

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
    @Column(name = "descricao", nullable = false)
    private String descricao;
    @Column(name = "status", length = 9)
    private StatusTarefaEnum statusTarefa;
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;
    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento;

    @Column(name = "id_usuario")
    private Long idUsuario;
}
