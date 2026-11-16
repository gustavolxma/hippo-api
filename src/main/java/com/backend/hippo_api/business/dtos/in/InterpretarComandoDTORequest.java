package com.backend.hippo_api.business.dtos.in;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterpretarComandoDTORequest {
    // Frase falada pelo Usuário, já transcrita pelo celular
    @NotBlank(message = "O Texto do comando é obrigatório!")
    @Size(max = 500, message = "O Texto do comando deve ter no máximo 500 caracteres!")
    private String texto;
    // Data e hora do celular no momento da fala. Necessário para entender "amanhã", "às 15h"...,
    // já que o servidor pode estar em outro fuso horário. Se não vier, usa o horário do servidor.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime agora;
}
