package com.backend.hippo_api.infrastructure.exceptions;

// Pedido que não faz sentido do jeito que veio (ex.: alterar a conta sem informar nada para alterar)
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    public RequisicaoInvalidaException(String mensagem, Throwable throwable) {
        super(mensagem, throwable);
    }
}
