package com.backend.hippo_api.infrastructure.security;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class JwtUtilTest {

    private static final String SEGREDO = "um-segredo-de-teste-com-mais-de-32-caracteres";

    @Test
    void tokenGuardaOEmailEValePeloTempoConfigurado() {
        JwtUtil jwtUtil = new JwtUtil(SEGREDO, 168);

        String token = jwtUtil.generateToken("maria@email.com");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("maria@email.com");
        Date expiracao = jwtUtil.extractClaims(token).getExpiration();
        long esperado = System.currentTimeMillis() + Duration.ofHours(168).toMillis();
        assertThat(expiracao.getTime()).isCloseTo(esperado, within(5_000L));
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRecusado() {
        String token = new JwtUtil("outro-segredo-de-teste-com-mais-de-32-caracteres", 1).generateToken("maria@email.com");

        assertThatThrownBy(() -> new JwtUtil(SEGREDO, 1).extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpiradoEhRecusado() {
        String token = new JwtUtil(SEGREDO, 0).generateToken("maria@email.com");

        assertThatThrownBy(() -> new JwtUtil(SEGREDO, 0).extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void semSegredoUsaChaveTemporaria() {
        JwtUtil jwtUtil = new JwtUtil("", 1);

        assertThat(jwtUtil.extractUsername(jwtUtil.generateToken("maria@email.com"))).isEqualTo("maria@email.com");
    }

    @Test
    void segredoCurtoImpedeAApiDeSubir() {
        assertThatThrownBy(() -> new JwtUtil("curto", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
