package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.InterpretacaoComandoService;
import com.backend.hippo_api.business.dtos.out.InterpretacaoComandoDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.enums.PrioridadeTarefaEnum;
import com.backend.hippo_api.infrastructure.exceptions.MuitasRequisicoesException;
import com.backend.hippo_api.infrastructure.security.JwtUtil;
import com.backend.hippo_api.infrastructure.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IaController.class)
@Import(SecurityConfig.class)
class IaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterpretacaoComandoService interpretacaoComandoService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private final Usuario maria = Usuario.builder().id(1L).nome("Maria").email("maria@email.com").senha("hash").build();

    private static final String COMANDO = """
            {"texto": "Me lembra de enviar o relatório amanhã às 15h", "agora": "16-11-2026 09:00:00"}
            """;

    @Test
    void exigeLogin() throws Exception {
        mockMvc.perform(post("/ia/interpretar").contentType(MediaType.APPLICATION_JSON).content(COMANDO))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void devolveOQueFoiEntendidoNosFormatosDaApi() throws Exception {
        when(interpretacaoComandoService.interpretar(any())).thenReturn(InterpretacaoComandoDTOResponse.builder()
                .entendeu(true)
                .nomeTarefa("Enviar o relatório")
                .data(LocalDate.of(2026, 11, 17))
                .hora(LocalTime.of(15, 0))
                .prioridade(PrioridadeTarefaEnum.ALTA)
                .lembreteMinutosAntes(0)
                .build());

        mockMvc.perform(post("/ia/interpretar").with(user(maria)).contentType(MediaType.APPLICATION_JSON).content(COMANDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entendeu").value(true))
                .andExpect(jsonPath("$.nomeTarefa").value("Enviar o relatório"))
                .andExpect(jsonPath("$.data").value("17-11-2026"))
                .andExpect(jsonPath("$.hora").value("15:00"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.lembreteMinutosAntes").value(0));
    }

    @Test
    void textoVazioEhRecusado() throws Exception {
        mockMvc.perform(post("/ia/interpretar").with(user(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cotaDaIaEsgotadaVira429() throws Exception {
        when(interpretacaoComandoService.interpretar(any())).thenThrow(new MuitasRequisicoesException("Limite"));

        mockMvc.perform(post("/ia/interpretar").with(user(maria)).contentType(MediaType.APPLICATION_JSON).content(COMANDO))
                .andExpect(status().isTooManyRequests());
    }
}
