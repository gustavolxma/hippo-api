package com.backend.hippo_api.business.dtos.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioCadastroDTORequest {
    @NotBlank(message = "O Nome é obrigatório!")
    private String nome;
    @NotBlank(message = "O Email é obrigatório!")
    @Email(message = "O Email deve estar em um formato válido!")
    private String email;
    @NotBlank(message = "A Senha é obrigatória!")
    @Size(min = 8, message = "A Senha deve ter no mínimo 8 caracteres!")
    private String senha;
}
