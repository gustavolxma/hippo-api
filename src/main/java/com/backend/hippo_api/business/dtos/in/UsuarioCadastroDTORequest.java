package com.backend.hippo_api.business.dtos.in;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioCadastroDTORequest {
    @NotBlank(message = "O Nome é obrigatório")
    private String nome;
    @NotBlank(message = "O Email é obrigatório")
    private String email;
    @NotBlank(message = "A Senha é obrigatória")
    private String senha;
}
