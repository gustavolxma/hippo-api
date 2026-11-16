package com.backend.hippo_api.infrastructure.exceptions;

// Falha em um serviço de fora da API (envio de Email, IA...), que não é culpa do Usuário
public class ServicoExternoException extends RuntimeException {
    public ServicoExternoException(String mensagem) {
        super(mensagem);
    }

    public ServicoExternoException(String mensagem, Throwable throwable) {
        super(mensagem, throwable);
    }
}
