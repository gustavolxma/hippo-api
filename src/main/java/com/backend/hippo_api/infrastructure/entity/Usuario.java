package com.backend.hippo_api.infrastructure.entity;

import jakarta.persistence.*;

import lombok.*;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;
    @Column(name = "email", length = 100, nullable = false)
    private String email;
    @Column(name = "senha", nullable = false)
    private String senha;
    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;
    // Vira true quando o Usuário confirma o Código enviado por Email (no Cadastro ou no Login).
    // O DEFAULT permite adicionar a coluna em bancos que já têm Usuários.
    @Builder.Default
    @Column(name = "email_verificado", nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerificado = false;

    @OneToMany(mappedBy = "usuario")
    private List<Tarefa> tarefas;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public @Nullable String getPassword() {
        return this.senha;
    }
}
