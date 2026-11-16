package com.backend.hippo_api.infrastructure.email;

import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

// Modo de desenvolvimento: em vez de enviar o Email, escreve o Código no console da API.
// Nunca use em produção: qualquer um com acesso aos logs veria os Códigos.
@Slf4j
@Service
@ConditionalOnProperty(name = "hippo.email.provedor", havingValue = "log", matchIfMissing = true)
public class LogEmailService implements EmailService {

    @Override
    public void enviarCodigoVerificacao(String email, String nome, String codigo, FinalidadeCodigoEnum finalidade) {
        log.warn("[EMAIL SIMULADO] Código de {} para {}: {}", finalidade, email, codigo);
    }
}
