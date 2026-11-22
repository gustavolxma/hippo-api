package com.backend.hippo_api.infrastructure.ia;

import com.backend.hippo_api.infrastructure.exceptions.MuitasRequisicoesException;
import com.backend.hippo_api.infrastructure.exceptions.ServicoExternoException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Simula a API do Gemini para conferir o que é enviado e como a resposta é lida
class GeminiClientTest {

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/modelo-teste:generateContent";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

    private GeminiClient client(String apiKey) {
        return new GeminiClient(builder, apiKey, "modelo-teste");
    }

    @Test
    void enviaInstrucoesTextoEEsquemaEDevolveOJsonGerado() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "chave"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("instruções"))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("comprar pão"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.type").value("OBJECT"))
                .andRespond(withSuccess("""
                        {"candidates": [{"content": {"parts": [{"text": "{\\"entendeu\\": true}"}], "role": "model"},
                          "finishReason": "STOP"}], "usageMetadata": {"totalTokenCount": 10}}
                        """, MediaType.APPLICATION_JSON));

        String json = client("chave").gerarJson("instruções", "comprar pão", Map.of("type", "OBJECT"));

        assertThat(json).isEqualTo("{\"entendeu\": true}");
        servidor.verify();
    }

    @Test
    void limiteGratuitoAtingidoVira429ParaOApp() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client("chave").gerarJson("i", "t", Map.of()))
                .isInstanceOf(MuitasRequisicoesException.class);
    }

    @Test
    void erroDoGeminiViraServicoIndisponivel() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client("chave").gerarJson("i", "t", Map.of()))
                .isInstanceOf(ServicoExternoException.class);
    }

    @Test
    void respostaSemCandidatosViraServicoIndisponivel() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client("chave").gerarJson("i", "t", Map.of()))
                .isInstanceOf(ServicoExternoException.class);
    }

    @Test
    void semChaveConfiguradaNemChamaOGemini() {
        assertThatThrownBy(() -> client("").gerarJson("i", "t", Map.of()))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("GEMINI_API_KEY");
        servidor.verify();
    }
}
