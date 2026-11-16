package com.backend.hippo_api.business.dtos.out;

import com.backend.hippo_api.infrastructure.enums.PrioridadeTarefaEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

// O que a IA entendeu da frase. O app mostra isso no cartão "Entendi! Nova tarefa" para o Usuário
// confirmar ou editar; a Tarefa só é criada depois, pelo POST /tarefas.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterpretacaoComandoDTOResponse {
    // false quando a frase não descreve uma Tarefa (os demais campos vêm vazios)
    private boolean entendeu;
    private String nomeTarefa;
    private String descricao;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate data;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime hora;
    private PrioridadeTarefaEnum prioridade;
    // Minutos antes do horário para avisar (0 = na hora). Nulo quando não pediu lembrete ou não há horário.
    private Integer lembreteMinutosAntes;
}
