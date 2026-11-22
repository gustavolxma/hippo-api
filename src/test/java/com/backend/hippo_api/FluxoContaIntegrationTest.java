package com.backend.hippo_api;

import com.backend.hippo_api.infrastructure.email.EmailService;
import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fluxo completo da conta com tudo real (banco H2, JWT, segurança, transações),
// capturando o Código que seria enviado por Email.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fluxo;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class FluxoContaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper jsonMapper;
    @MockitoSpyBean
    private EmailService emailService;

    private String ultimoCodigo(String email, FinalidadeCodigoEnum finalidade) {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarCodigoVerificacao(eq(email), anyString(), codigo.capture(), eq(finalidade));
        clearInvocations(emailService);
        return codigo.getValue();
    }

    private String json(String formato, Object... valores) {
        return formato.formatted(valores);
    }

    private String token(String respostaJson) {
        return jsonMapper.readTree(respostaJson).get("token").asString();
    }

    @Test
    void cadastroLoginEAlteracaoDeSenhaComCodigos() throws Exception {
        String email = "maria@email.com";

        // 1. Cadastro: cria a conta e envia o Código
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"nome\": \"Maria\", \"email\": \"%s\", \"senha\": \"senha1234\"}", email)))
                .andExpect(status().isCreated());
        String codigoCadastro = ultimoCodigo(email, FinalidadeCodigoEnum.CADASTRO);

        // Código errado é recusado; o certo devolve o Token
        mockMvc.perform(post("/usuarios/verificar-cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"email\": \"%s\", \"codigo\": \"%s\"}", email,
                                codigoCadastro.equals("000000") ? "111111" : "000000")))
                .andExpect(status().isBadRequest());
        String tokenCadastro = token(mockMvc.perform(post("/usuarios/verificar-cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"email\": \"%s\", \"codigo\": \"%s\"}", email, codigoCadastro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andReturn().getResponse().getContentAsString());

        // O Token funciona nas rotas protegidas
        mockMvc.perform(get("/tarefas").header("Authorization", tokenCadastro)).andExpect(status().isOk());

        // 2. Login: Email e Senha certos enviam o Código; o Código devolve o Token
        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"email\": \"%s\", \"senha\": \"senha1234\"}", email)))
                .andExpect(status().isAccepted());
        String tokenLogin = token(mockMvc.perform(post("/usuarios/login/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"email\": \"%s\", \"codigo\": \"%s\"}", email, ultimoCodigo(email, FinalidadeCodigoEnum.LOGIN))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // 3. Alterar a Senha com o Código enviado ao Email atual
        mockMvc.perform(post("/usuarios/conta/codigo").header("Authorization", tokenLogin))
                .andExpect(status().isAccepted());
        mockMvc.perform(put("/usuarios").header("Authorization", tokenLogin).contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"codigo\": \"%s\", \"novaSenha\": \"novasenha99\"}",
                                ultimoCodigo(email, FinalidadeCodigoEnum.ALTERAR_CONTA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // A Senha antiga não entra mais; a nova sim
        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"email\": \"%s\", \"senha\": \"senha1234\"}", email)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"email\": \"%s\", \"senha\": \"novasenha99\"}", email)))
                .andExpect(status().isAccepted());
    }

    @Test
    void healthEhPublicoETokenInvalidoRecebe401() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Sem Token ou com Token inválido o app recebe 401 (e não 500), para pedir login de novo
        mockMvc.perform(get("/tarefas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/tarefas").header("Authorization", "Bearer token.invalido.qualquer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cincoCodigosErradosInvalidamOCodigoCerto() throws Exception {
        String email = "joao@email.com";
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"nome\": \"João\", \"email\": \"%s\", \"senha\": \"senha1234\"}", email)))
                .andExpect(status().isCreated());
        String certo = ultimoCodigo(email, FinalidadeCodigoEnum.CADASTRO);
        String errado = certo.equals("000000") ? "111111" : "000000";

        // As tentativas erradas precisam ficar salvas mesmo com o erro (não podem ser desfeitas)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/usuarios/verificar-cadastro").contentType(MediaType.APPLICATION_JSON)
                            .content(json("{\"email\": \"%s\", \"codigo\": \"%s\"}", email, errado)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/usuarios/verificar-cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"email\": \"%s\", \"codigo\": \"%s\"}", email, certo)))
                .andExpect(status().isBadRequest());
    }
}
