package com.backend.hippo_api.business.dtos.in;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarefaCadastroDTORequest {
    @NotBlank(message = "O Nome da Tarefa é obrigatório!")
    private String nomeTarefa;
    @NotBlank(message = "A Descrição da Tarefa é obrigatória!")
    private String descricao;
    @NotNull(message = "A Data do Evento é obrigatória!")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime dataEvento;
}
