package com.backend.hippo_api.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
public class JwtUtil {

    // O HS256 exige uma chave de pelo menos 256 bits (32 bytes)
    private static final int TAMANHO_MINIMO_CHAVE = 32;

    // Chave usada para assinar e verificar os Tokens
    private final SecretKey chave;
    // Por quanto tempo o Token vale depois de emitido
    private final Duration validade;

    public JwtUtil(@Value("${hippo.jwt.secret:}") String segredo,
                   @Value("${hippo.jwt.validade-horas:168}") long validadeHoras) {
        this.chave = criarChave(segredo);
        this.validade = Duration.ofHours(validadeHoras);
    }

    private static SecretKey criarChave(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            // Sem JWT_SECRET (ex.: rodando no computador): chave aleatória que só vale até a API reiniciar
            log.warn("JWT_SECRET não configurado: usando uma chave temporária. Os Tokens deixam de valer quando a API reiniciar.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_CHAVE) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos " + TAMANHO_MINIMO_CHAVE + " caracteres.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    // Gera um Token com o Email do Usuário, válido pelo tempo configurado
    public String generateToken(String username) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username) // O Email do Usuário identifica o dono do Token
                .setIssuedAt(new Date(agora))
                .setExpiration(new Date(agora + validade.toMillis()))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extrai as claims do Token (lança JwtException se for inválido ou estiver expirado)
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Extrai o Email do Usuário do Token
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }
}
