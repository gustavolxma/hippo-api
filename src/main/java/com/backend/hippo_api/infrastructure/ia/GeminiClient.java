package com.backend.hippo_api.infrastructure.ia;

import com.backend.hippo_api.infrastructure.exceptions.MuitasRequisicoesException;
import com.backend.hippo_api.infrastructure.exceptions.ServicoExternoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

// Cliente da API do Gemini (Google AI Studio), chamada por HTTP.
// Pede a resposta em JSON seguindo um esquema, para a IA devolver só os campos esperados.
@Slf4j
@Component
public class GeminiClient {

    private static final String URL_GEMINI = "https://generativelanguage.googleapis.com/v1beta";

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;

    public GeminiClient(RestClient.Builder restClientBuilder,
                        @Value("${hippo.ia.gemini.api-key:}") String apiKey,
                        @Value("${hippo.ia.gemini.modelo}") String modelo) {
        this.restClient = restClientBuilder.baseUrl(URL_GEMINI).build();
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    // Envia as instruções e o texto do Usuário e devolve o JSON gerado pela IA (como texto)
    public String gerarJson(String instrucoes, String texto, Map<String, Object> esquemaResposta) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServicoExternoException("A IA não está configurada no servidor (falta a GEMINI_API_KEY).");
        }

        RequisicaoGemini corpo = new RequisicaoGemini(
                new Conteudo(null, List.of(new Parte(instrucoes))),
                List.of(new Conteudo("user", List.of(new Parte(texto)))),
                new ConfiguracaoGeracao(0.0, "application/json", esquemaResposta)
        );

        try {
            RespostaGemini resposta = restClient.post()
                    .uri("/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(RespostaGemini.class);

            return resposta.textoGerado();
        }
        catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new MuitasRequisicoesException("O limite gratuito da IA foi atingido. Tente novamente em instantes.");
            }
            log.error("Gemini recusou a requisição: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServicoExternoException("Não foi possível interpretar o comando agora.", e);
        }
        catch (RestClientException | IllegalStateException e) {
            log.error("Falha ao chamar o Gemini", e);
            throw new ServicoExternoException("Não foi possível interpretar o comando agora.", e);
        }
    }

    // Formato do corpo esperado pela API do Gemini (generateContent)
    private record Parte(String text) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Conteudo(String role, List<Parte> parts) {
    }

    private record ConfiguracaoGeracao(Double temperature, String responseMimeType, Map<String, Object> responseSchema) {
    }

    private record RequisicaoGemini(Conteudo systemInstruction, List<Conteudo> contents, ConfiguracaoGeracao generationConfig) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ParteResposta(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConteudoResposta(List<ParteResposta> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidato(ConteudoResposta content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespostaGemini(List<Candidato> candidates) {
        String textoGerado() {
            if (candidates == null || candidates.isEmpty() || candidates.get(0).content() == null
                    || candidates.get(0).content().parts() == null || candidates.get(0).content().parts().isEmpty()) {
                throw new IllegalStateException("O Gemini não devolveu nenhuma resposta");
            }
            return candidates.get(0).content().parts().get(0).text();
        }
    }
}
