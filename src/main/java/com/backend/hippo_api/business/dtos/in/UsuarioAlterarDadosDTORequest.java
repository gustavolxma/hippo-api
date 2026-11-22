package com.backend.hippo_api.business.dtos.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

// Alterar Email e/ou Senha: só o que vier preenchido é alterado, sempre com o Código enviado ao Email atual
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioAlterarDadosDTORequest {
    @NotBlank(message = "O Código é obrigatório!")
    @Pattern(regexp = "\\d{6}", message = "O Código deve ter 6 dígitos!")
    private String codigo;
    @Email(message = "O Novo Email deve estar em um formato válido!")
    private String novoEmail;
    @Size(min = 8, message = "A Nova Senha deve ter no mínimo 8 caracteres!")
    private String novaSenha;
}
