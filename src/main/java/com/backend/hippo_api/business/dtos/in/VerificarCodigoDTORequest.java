package com.backend.hippo_api.business.dtos.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificarCodigoDTORequest {
    @NotBlank(message = "O Email é obrigatório!")
    @Email(message = "O Email deve estar em um formato válido!")
    private String email;
    @NotBlank(message = "O Código é obrigatório!")
    @Pattern(regexp = "\\d{6}", message = "O Código deve ter 6 dígitos!")
    private String codigo;
}
