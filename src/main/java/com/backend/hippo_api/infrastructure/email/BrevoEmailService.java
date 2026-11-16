package com.backend.hippo_api.infrastructure.email;

import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;
import com.backend.hippo_api.infrastructure.exceptions.ServicoExternoException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

// Envio real de Email pela API HTTP da Brevo (https://www.brevo.com).
// Usamos HTTP e não SMTP porque o plano gratuito do Render bloqueia as portas de SMTP.
@Slf4j
@Service
@ConditionalOnProperty(name = "hippo.email.provedor", havingValue = "brevo")
public class BrevoEmailService implements EmailService {

    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String remetenteEmail;
    private final String remetenteNome;

    public BrevoEmailService(RestClient.Builder restClientBuilder,
                             @Value("${hippo.email.brevo.api-key}") String apiKey,
                             @Value("${hippo.email.remetente-email}") String remetenteEmail,
                             @Value("${hippo.email.remetente-nome:Hippo}") String remetenteNome) {
        this.restClient = restClientBuilder
                .baseUrl(URL_BREVO)
                .defaultHeader("api-key", apiKey)
                .build();
        this.remetenteEmail = remetenteEmail;
        this.remetenteNome = remetenteNome;
    }

    @Override
    public void enviarCodigoVerificacao(String email, String nome, String codigo, FinalidadeCodigoEnum finalidade) {
        EmailBrevo corpo = new EmailBrevo(
                new Contato(remetenteNome, remetenteEmail),
                List.of(new Contato(nome, email)),
                assunto(finalidade),
                html(nome, codigo, finalidade)
        );

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
        }
        catch (RestClientException e) {
            log.error("Falha ao enviar Email pela Brevo para {}", email, e);
            throw new ServicoExternoException("Não foi possível enviar o Email com o Código. Tente novamente.", e);
        }
    }

    private String assunto(FinalidadeCodigoEnum finalidade) {
        return switch (finalidade) {
            case CADASTRO -> "Confirme seu cadastro no Hippo";
            case LOGIN -> "Seu código de acesso ao Hippo";
            case ALTERAR_CONTA -> "Código para alterar seu email ou senha no Hippo";
        };
    }

    private String html(String nome, String codigo, FinalidadeCodigoEnum finalidade) {
        String motivo = switch (finalidade) {
            case CADASTRO -> "confirmar seu cadastro";
            case LOGIN -> "entrar na sua conta";
            case ALTERAR_CONTA -> "alterar seu email ou senha";
        };
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px">
                  <p>Olá, %s!</p>
                  <p>Use o código abaixo para %s no Hippo:</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:6px;color:#6B46F0">%s</p>
                  <p>O código expira em 10 minutos. Se não foi você, ignore este email.</p>
                </div>
                """.formatted(nome, motivo, codigo);
    }

    // Formato do corpo esperado pela API da Brevo
    private record Contato(String name, String email) {
    }

    private record EmailBrevo(Contato sender, List<Contato> to, String subject, String htmlContent) {
    }
}
