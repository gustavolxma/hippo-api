package com.backend.hippo_api.business.dtos.out;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenDTOResponse {
    // Já no formato do header Authorization: "Bearer eyJ..."
    private String token;
    private UsuarioBuscaDadosDTOResponse usuario;
}
