package com.backend.hippo_api.infrastructure.exceptions;

// Usada quando o Usuário pede algo rápido demais (ex.: reenviar o Código antes do tempo de espera)
public class MuitasRequisicoesException extends RuntimeException {
    public MuitasRequisicoesException(String mensagem) {
        super(mensagem);
    }

    public MuitasRequisicoesException(String mensagem, Throwable throwable) {
        super(mensagem, throwable);
    }
}
