package com.backend.hippo_api.infrastructure.email;

import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;

// Envio dos Códigos de Verificação. A implementação usada depende da propriedade hippo.email.provedor:
// "log" (padrão, só escreve o Código no console, para desenvolver) ou "brevo" (envio real por API HTTP).
public interface EmailService {
    void enviarCodigoVerificacao(String email, String nome, String codigo, FinalidadeCodigoEnum finalidade);
}
