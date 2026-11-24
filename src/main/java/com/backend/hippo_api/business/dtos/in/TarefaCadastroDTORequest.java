package com.backend.hippo_api.business.dtos.in;

import com.backend.hippo_api.infrastructure.enums.PrioridadeTarefaEnum;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarefaCadastroDTORequest {

    @NotBlank(message = "O Nome da Tarefa é obrigatório!")
    @Size(max = 255, message = "O Nome da Tarefa deve ter no máximo 255 caracteres!")
    private String nomeTarefa;

    // Opcional
    @Size(max = 2000, message = "A Descrição da Tarefa deve ter no máximo 2000 caracteres!")
    private String descricao;

    // Opcional: uma Tarefa pode não ter data. Datas passadas são aceitas, porque Tarefas
    // atrasadas também são editadas e uma Tarefa de hoje sem hora fica com 00:00.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime dataEvento;

    // false quando a Tarefa tem só o dia (a hora de dataEvento é ignorada). Se não vier, vale true.
    private Boolean temHora;

    // Se não vier, vale MEDIA
    private PrioridadeTarefaEnum prioridade;

    // Minutos antes do horário para avisar (0 = na hora). Nulo = sem lembrete.
    @PositiveOrZero(message = "O Lembrete não pode ser negativo!")
    private Integer lembreteMinutosAntes;
}
