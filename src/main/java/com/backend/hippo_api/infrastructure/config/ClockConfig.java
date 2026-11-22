package com.backend.hippo_api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // Relógio da aplicação. Injetado (em vez de LocalDateTime.now() direto) para os testes controlarem o tempo.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
