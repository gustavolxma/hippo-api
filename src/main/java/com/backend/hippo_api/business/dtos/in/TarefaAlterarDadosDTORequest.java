package com.backend.hippo_api.business.dtos.in;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarefaAlterarDadosDTORequest {
    @Pattern(regexp = ".*\\S.*", message = "O Nome da Tarefa não pode ser vazio!")
    private String nomeTarefa;
    @Pattern(regexp = ".*\\S.*", message = "A Descrição da Tarefa não pode ser vazia!")
    private String descricao;
    @FutureOrPresent(message = "A Data da Tarefa deve ser atual ou futura!")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime dataEvento;
}
