package com.backend.hippo_api.business.dtos.out;

import com.backend.hippo_api.infrastructure.enums.StatusTarefaEnum;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarefaCadastroDTOResponse {
    private Long id;
    private String nomeTarefa;
    private String descricao;
    private StatusTarefaEnum statusTarefa;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime dataCriacao;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime dataEvento;
}
