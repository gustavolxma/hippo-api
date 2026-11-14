package com.backend.hippo_api.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtUtil {

    // Chave secreta usada para assinar e verificar tokens JWT
    private final String secretKey = "sua-chave-secreta-super-segura-que-deve-ser-bem-longa";

    // Gera um token JWT com o nome de usuário e validade de 1 hora
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // Define o nome de usuário como o assunto do token
                .setIssuedAt(new Date()) // Define a data e hora de emissão do token
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // Define a data e hora de expiração (1 hora a partir da emissão)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256) // Converte a chave secreta em bytes e assina o token com ela
                .compact(); // Constrói o token JWT
    }

    // Extrai as claims do token JWT (informações adicionais do token)
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))) // Define a chave secreta para validar a assinatura do token
                .build()
                .parseClaimsJws(token) // Analisa o token JWT e obtém as claims
                .getBody(); // Retorna o corpo das claims
    }

    // Extrai o Email do Usuário do token JWT
    public String extractUsername(String token) {
        // Obtém o assunto (Email do Usuário) das claims do token
        return extractClaims(token).getSubject();
    }
}
