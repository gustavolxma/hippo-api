package com.backend.hippo_api.business;

import com.backend.hippo_api.business.converter.UsuarioConverter;
import com.backend.hippo_api.business.dtos.in.ReenviarCodigoDTORequest;
import com.backend.hippo_api.business.dtos.in.UsuarioAlterarDadosDTORequest;
import com.backend.hippo_api.business.dtos.in.UsuarioCadastroDTORequest;
import com.backend.hippo_api.business.dtos.in.VerificarCodigoDTORequest;
import com.backend.hippo_api.business.dtos.out.TokenDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;
import com.backend.hippo_api.infrastructure.exceptions.CodigoInvalidoException;
import com.backend.hippo_api.infrastructure.exceptions.ConflictException;
import com.backend.hippo_api.infrastructure.exceptions.RequisicaoInvalidaException;
import com.backend.hippo_api.infrastructure.repository.UsuarioRepository;
import com.backend.hippo_api.infrastructure.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CodigoVerificacaoService codigoVerificacaoService;
    @Mock
    private JwtUtil jwtUtil;

    private UsuarioService service;

    private final UsuarioCadastroDTORequest cadastro = new UsuarioCadastroDTORequest("Maria", "maria@email.com", "senha1234");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneId.of("UTC"));
        service = new UsuarioService(usuarioRepository, new UsuarioConverter(), new BCryptPasswordEncoder(),
                codigoVerificacaoService, jwtUtil, clock);
        // save devolve o próprio Usuário recebido
        lenient().when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Usuario usuario(boolean verificado) {
        return Usuario.builder().id(1L).nome("Maria").email("maria@email.com").senha("hash").emailVerificado(verificado).build();
    }

    @Test
    void cadastroNovoFicaNaoVerificadoEEnviaCodigo() {
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());

        service.cadastrarUsuario(cadastro);

        verify(usuarioRepository).save(argThat(u ->
                !u.isEmailVerificado() && !u.getSenha().equals("senha1234")));
        verify(codigoVerificacaoService).gerarEEnviarCodigo("maria@email.com", "Maria", FinalidadeCodigoEnum.CADASTRO);
    }

    @Test
    void emailJaVerificadoNaoPodeSerCadastradoDeNovo() {
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> service.cadastrarUsuario(cadastro)).isInstanceOf(ConflictException.class);
        verify(codigoVerificacaoService, never()).gerarEEnviarCodigo(any(), any(), any());
    }

    @Test
    void cadastroNuncaConfirmadoEReaproveitadoComOsDadosNovos() {
        Usuario pendente = usuario(false);
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(pendente));

        service.cadastrarUsuario(new UsuarioCadastroDTORequest("Maria Silva", "maria@email.com", "outrasenha"));

        assertThat(pendente.getNome()).isEqualTo("Maria Silva");
        verify(codigoVerificacaoService).gerarEEnviarCodigo("maria@email.com", "Maria Silva", FinalidadeCodigoEnum.CADASTRO);
    }

    @Test
    void codigoDeLoginCertoDevolveTokenEMarcaEmailComoVerificado() {
        Usuario maria = usuario(false);
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(maria));
        when(jwtUtil.generateToken("maria@email.com")).thenReturn("jwt");

        TokenDTOResponse resposta = service.verificarLogin(new VerificarCodigoDTORequest("maria@email.com", "123456"));

        verify(codigoVerificacaoService).validarCodigo("maria@email.com", FinalidadeCodigoEnum.LOGIN, "123456");
        assertThat(resposta.getToken()).isEqualTo("Bearer jwt");
        assertThat(resposta.getUsuario().getEmail()).isEqualTo("maria@email.com");
        assertThat(maria.isEmailVerificado()).isTrue();
    }

    @Test
    void codigoErradoNaoGeraToken() {
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario(true)));
        doThrow(new CodigoInvalidoException("Código inválido ou expirado!"))
                .when(codigoVerificacaoService).validarCodigo("maria@email.com", FinalidadeCodigoEnum.LOGIN, "000000");

        assertThatThrownBy(() -> service.verificarLogin(new VerificarCodigoDTORequest("maria@email.com", "000000")))
                .isInstanceOf(CodigoInvalidoException.class);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void emailDesconhecidoNaVerificacaoPareceCodigoInvalido() {
        when(usuarioRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificarCadastro(new VerificarCodigoDTORequest("ninguem@email.com", "123456")))
                .isInstanceOf(CodigoInvalidoException.class);
    }

    @Test
    void soReenviaQuandoHaCodigoPendente() {
        when(codigoVerificacaoService.existeCodigoPendente("maria@email.com", FinalidadeCodigoEnum.LOGIN)).thenReturn(false);

        assertThatThrownBy(() -> service.reenviarCodigo(
                new ReenviarCodigoDTORequest("maria@email.com", FinalidadeCodigoEnum.LOGIN)))
                .isInstanceOf(CodigoInvalidoException.class);
        verify(codigoVerificacaoService, never()).gerarEEnviarCodigo(any(), any(), any());
    }

    @Test
    void alterarSoASenhaMantemOEmailEDevolveTokenNovo() {
        Usuario maria = usuario(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(maria));
        when(jwtUtil.generateToken("maria@email.com")).thenReturn("novo-jwt");

        TokenDTOResponse resposta = service.alterarDadosUsuario(maria,
                new UsuarioAlterarDadosDTORequest("123456", "maria@email.com", "novasenha123"));

        verify(codigoVerificacaoService).validarCodigo("maria@email.com", FinalidadeCodigoEnum.ALTERAR_CONTA, "123456");
        assertThat(new BCryptPasswordEncoder().matches("novasenha123", maria.getSenha())).isTrue();
        assertThat(maria.getEmail()).isEqualTo("maria@email.com");
        assertThat(resposta.getToken()).isEqualTo("Bearer novo-jwt");
    }

    @Test
    void alterarOEmailGeraTokenComOEmailNovo() {
        Usuario maria = usuario(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(maria));
        when(usuarioRepository.existsByEmail("nova@email.com")).thenReturn(false);
        when(jwtUtil.generateToken("nova@email.com")).thenReturn("jwt-novo-email");

        TokenDTOResponse resposta = service.alterarDadosUsuario(maria,
                new UsuarioAlterarDadosDTORequest("123456", "nova@email.com", null));

        // O Código é sempre o enviado para o Email ATUAL
        verify(codigoVerificacaoService).validarCodigo("maria@email.com", FinalidadeCodigoEnum.ALTERAR_CONTA, "123456");
        assertThat(maria.getEmail()).isEqualTo("nova@email.com");
        assertThat(maria.getSenha()).isEqualTo("hash");
        assertThat(resposta.getToken()).isEqualTo("Bearer jwt-novo-email");
    }

    @Test
    void alterarSemNadaParaMudarERecusadoSemGastarOCodigo() {
        Usuario maria = usuario(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(maria));

        assertThatThrownBy(() -> service.alterarDadosUsuario(maria,
                new UsuarioAlterarDadosDTORequest("123456", "maria@email.com", "")))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verify(codigoVerificacaoService, never()).validarCodigo(any(), any(), any());
    }

    @Test
    void alterarParaEmailDeOutraContaDaConflito() {
        Usuario maria = usuario(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(maria));
        when(usuarioRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.alterarDadosUsuario(maria,
                new UsuarioAlterarDadosDTORequest("123456", "joao@email.com", null)))
                .isInstanceOf(ConflictException.class);
        verify(codigoVerificacaoService, never()).validarCodigo(any(), any(), any());
    }

    @Test
    void codigoErradoNaoAlteraNada() {
        Usuario maria = usuario(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(maria));
        doThrow(new CodigoInvalidoException("Código inválido ou expirado!")).when(codigoVerificacaoService)
                .validarCodigo("maria@email.com", FinalidadeCodigoEnum.ALTERAR_CONTA, "000000");

        assertThatThrownBy(() -> service.alterarDadosUsuario(maria,
                new UsuarioAlterarDadosDTORequest("000000", null, "novasenha123")))
                .isInstanceOf(CodigoInvalidoException.class);
        assertThat(maria.getSenha()).isEqualTo("hash");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reenviarCodigoDeAlterarContaNaoEPermitidoSemToken() {
        assertThatThrownBy(() -> service.reenviarCodigo(
                new ReenviarCodigoDTORequest("maria@email.com", FinalidadeCodigoEnum.ALTERAR_CONTA)))
                .isInstanceOf(CodigoInvalidoException.class);
    }
}
