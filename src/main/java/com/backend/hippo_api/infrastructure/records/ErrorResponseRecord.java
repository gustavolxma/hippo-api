package com.backend.hippo_api.infrastructure.records;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponseRecord(
        int status,
        String mensagem,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime momentoExcecao
) {
}
