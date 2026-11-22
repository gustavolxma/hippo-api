package com.backend.hippo_api.infrastructure.entity;

import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

// Código de 6 dígitos enviado por Email no Cadastro, no Login e na alteração de Email/Senha.
// Só o Hash do Código é guardado (igual à Senha), nunca o Código em texto.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "codigos_verificacao")
public class CodigoVerificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 100, nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(name = "finalidade", length = 30, nullable = false)
    private FinalidadeCodigoEnum finalidade;
    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;
    @Column(name = "tentativas", nullable = false)
    private int tentativas;
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;
}
