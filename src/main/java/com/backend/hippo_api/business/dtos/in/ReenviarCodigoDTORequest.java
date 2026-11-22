package com.backend.hippo_api.business.dtos.in;

import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReenviarCodigoDTORequest {
    @NotBlank(message = "O Email é obrigatório!")
    @Email(message = "O Email deve estar em um formato válido!")
    private String email;
    // CADASTRO ou LOGIN (o de ALTERAR_CONTA é pedido de novo pelo próprio endpoint autenticado)
    @NotNull(message = "A Finalidade do Código é obrigatória!")
    private FinalidadeCodigoEnum finalidade;
}
