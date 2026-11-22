package com.backend.hippo_api.infrastructure.exceptions;

public class CodigoInvalidoException extends RuntimeException {
    public CodigoInvalidoException(String mensagem) {
        super(mensagem);
    }

    public CodigoInvalidoException(String mensagem, Throwable throwable) {
        super(mensagem, throwable);
    }
}
