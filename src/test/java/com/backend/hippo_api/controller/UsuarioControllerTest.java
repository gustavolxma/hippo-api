package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.UsuarioService;
import com.backend.hippo_api.business.dtos.out.TokenDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.exceptions.CodigoInvalidoException;
import com.backend.hippo_api.infrastructure.exceptions.MuitasRequisicoesException;
import com.backend.hippo_api.infrastructure.security.JwtUtil;
import com.backend.hippo_api.infrastructure.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Testa as rotas de Cadastro/Login com a configuração de segurança real (sem Token nessas etapas)
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String CODIGO_JSON = """
            {"email": "maria@email.com", "codigo": "123456"}
            """;

    @Test
    void loginComSenhaCertaEnviaCodigoEResponde202() throws Exception {
        when(userDetailsService.loadUserByUsername("maria@email.com")).thenReturn(Usuario.builder()
                .email("maria@email.com")
                .senha(new BCryptPasswordEncoder().encode("senha1234"))
                .build());

        mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "maria@email.com", "senha": "senha1234"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").exists());

        verify(usuarioService).enviarCodigoLogin("maria@email.com");
    }

    @Test
    void verificarLoginNaoExigeTokenEDevolveOToken() throws Exception {
        when(usuarioService.verificarLogin(any())).thenReturn(TokenDTOResponse.builder().token("Bearer jwt").build());

        mockMvc.perform(post("/usuarios/login/verificar").contentType(MediaType.APPLICATION_JSON).content(CODIGO_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("Bearer jwt"));
    }

    @Test
    void verificarCadastroNaoExigeToken() throws Exception {
        when(usuarioService.verificarCadastro(any())).thenReturn(TokenDTOResponse.builder().token("Bearer jwt").build());

        mockMvc.perform(post("/usuarios/verificar-cadastro").contentType(MediaType.APPLICATION_JSON).content(CODIGO_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void codigoInvalidoVira400ComMensagem() throws Exception {
        when(usuarioService.verificarLogin(any())).thenThrow(new CodigoInvalidoException("Código inválido ou expirado!"));

        mockMvc.perform(post("/usuarios/login/verificar").contentType(MediaType.APPLICATION_JSON).content(CODIGO_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Código inválido ou expirado!"));
    }

    @Test
    void codigoComFormatoErradoEhRecusadoNaValidacao() throws Exception {
        mockMvc.perform(post("/usuarios/login/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "maria@email.com", "codigo": "12ab"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reenviarRapidoDemaisVira429() throws Exception {
        doThrow(new MuitasRequisicoesException("Aguarde")).when(usuarioService).reenviarCodigo(any());

        mockMvc.perform(post("/usuarios/codigo/reenviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "maria@email.com", "finalidade": "LOGIN"}
                                """))
                .andExpect(status().isTooManyRequests());
    }
}
