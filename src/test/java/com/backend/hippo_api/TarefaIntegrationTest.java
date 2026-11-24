package com.backend.hippo_api;

import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.repository.UsuarioRepository;
import com.backend.hippo_api.infrastructure.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Contrato das Tarefas usado pelo app, com tudo real (banco H2, JWT, segurança)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tarefas;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class TarefaIntegrationTest {

    private static final String EMAIL = "maria@email.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    void entrar() {
        if (usuarioRepository.findByEmail(EMAIL).isEmpty()) {
            usuarioRepository.save(Usuario.builder().nome("Maria").email(EMAIL).senha("hash")
                    .dataCadastro(LocalDateTime.now()).emailVerificado(true).build());
        }
        token = "Bearer " + jwtUtil.generateToken(EMAIL);
    }

    private long criar(String json) throws Exception {
        String resposta = mockMvc.perform(post("/tarefas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(resposta).get("id").asLong();
    }

    @Test
    void guardaPrioridadeHoraELembrete() throws Exception {
        long id = criar("""
                {"nomeTarefa": "Reunião", "descricao": "Sala 2", "dataEvento": "10-12-2030 15:00:00",
                 "temHora": true, "prioridade": "ALTA", "lembreteMinutosAntes": 30}""");

        mockMvc.perform(get("/tarefas/tarefa/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeTarefa").value("Reunião"))
                .andExpect(jsonPath("$.dataEvento").value("10-12-2030 15:00:00"))
                .andExpect(jsonPath("$.temHora").value(true))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.lembreteMinutosAntes").value(30));
    }

    @Test
    void soONomeEhObrigatorio() throws Exception {
        long id = criar("{\"nomeTarefa\": \"Estudar\"}");

        mockMvc.perform(get("/tarefas/tarefa/" + id).header("Authorization", token))
                .andExpect(jsonPath("$.descricao").value(""))
                .andExpect(jsonPath("$.dataEvento").isEmpty())
                .andExpect(jsonPath("$.temHora").value(false))
                .andExpect(jsonPath("$.prioridade").value("MEDIA"))
                .andExpect(jsonPath("$.lembreteMinutosAntes").isEmpty())
                .andExpect(jsonPath("$.statusTarefa").value("PENDENTE"));

        mockMvc.perform(post("/tarefas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nomeTarefa\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alterarSubstituiOsDadosEAceitaTarefaAtrasada() throws Exception {
        long id = criar("""
                {"nomeTarefa": "Antiga", "descricao": "Texto", "dataEvento": "10-01-2020 00:00:00",
                 "temHora": false, "prioridade": "BAIXA"}""");

        mockMvc.perform(put("/tarefas").param("id", String.valueOf(id)).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeTarefa\": \"Nova\", \"dataEvento\": \"10-01-2020 00:00:00\", \"temHora\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeTarefa").value("Nova"))
                .andExpect(jsonPath("$.descricao").value(""))
                .andExpect(jsonPath("$.temHora").value(false))
                .andExpect(jsonPath("$.prioridade").value("MEDIA"));

        // Sem dataEvento a data é removida
        mockMvc.perform(put("/tarefas").param("id", String.valueOf(id)).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nomeTarefa\": \"Nova\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataEvento").isEmpty());
    }

    @Test
    void concluirMantemOsDadosEExcluirRemove() throws Exception {
        long id = criar("{\"nomeTarefa\": \"Estudar\", \"prioridade\": \"ALTA\"}");

        mockMvc.perform(patch("/tarefas/" + id + "/status").param("status", "CONCLUIDA").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusTarefa").value("CONCLUIDA"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"));

        mockMvc.perform(delete("/tarefas").param("id", String.valueOf(id)).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/tarefas/tarefa/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}
