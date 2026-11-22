package com.backend.hippo_api.business;

import com.backend.hippo_api.infrastructure.email.EmailService;
import com.backend.hippo_api.infrastructure.entity.CodigoVerificacao;
import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;
import com.backend.hippo_api.infrastructure.exceptions.CodigoInvalidoException;
import com.backend.hippo_api.infrastructure.exceptions.MuitasRequisicoesException;
import com.backend.hippo_api.infrastructure.repository.CodigoVerificacaoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodigoVerificacaoServiceTest {

    private static final String EMAIL = "maria@email.com";
    private static final FinalidadeCodigoEnum LOGIN = FinalidadeCodigoEnum.LOGIN;

    @Mock
    private CodigoVerificacaoRepository codigoRepository;
    @Mock
    private EmailService emailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // Relógio fixo: 16/11/2026 12:00
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneId.of("UTC"));
    private final LocalDateTime agora = LocalDateTime.now(clock);

    private CodigoVerificacaoService service;

    @BeforeEach
    void setUp() {
        service = new CodigoVerificacaoService(codigoRepository, emailService, passwordEncoder, clock);
    }

    private CodigoVerificacao codigoSalvo(String codigo, LocalDateTime criadoEm, int tentativas) {
        return CodigoVerificacao.builder()
                .email(EMAIL)
                .finalidade(LOGIN)
                .codigoHash(passwordEncoder.encode(codigo))
                .tentativas(tentativas)
                .dataCriacao(criadoEm)
                .dataExpiracao(criadoEm.plus(CodigoVerificacaoService.VALIDADE))
                .build();
    }

    @Test
    void geraCodigoDeSeisDigitosGuardaSoOHashEEnviaPorEmail() {
        when(codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(EMAIL, LOGIN)).thenReturn(Optional.empty());

        service.gerarEEnviarCodigo(EMAIL, "Maria", LOGIN);

        ArgumentCaptor<String> codigoEnviado = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoVerificacao(eq(EMAIL), eq("Maria"), codigoEnviado.capture(), eq(LOGIN));
        ArgumentCaptor<CodigoVerificacao> salvo = ArgumentCaptor.forClass(CodigoVerificacao.class);
        verify(codigoRepository).save(salvo.capture());

        assertThat(codigoEnviado.getValue()).matches("\\d{6}");
        assertThat(salvo.getValue().getCodigoHash()).isNotEqualTo(codigoEnviado.getValue());
        assertThat(passwordEncoder.matches(codigoEnviado.getValue(), salvo.getValue().getCodigoHash())).isTrue();
        assertThat(salvo.getValue().getDataExpiracao()).isEqualTo(agora.plusMinutes(10));
        verify(codigoRepository).deleteByEmailAndFinalidade(EMAIL, LOGIN);
    }

    @Test
    void naoDeixaPedirOutroCodigoAntesDaEspera() {
        when(codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(EMAIL, LOGIN))
                .thenReturn(Optional.of(codigoSalvo("123456", agora.minusSeconds(10), 0)));

        assertThatThrownBy(() -> service.gerarEEnviarCodigo(EMAIL, "Maria", LOGIN))
                .isInstanceOf(MuitasRequisicoesException.class);
        verify(emailService, never()).enviarCodigoVerificacao(anyString(), anyString(), anyString(), any());
    }

    @Test
    void codigoCertoEApagadoDepoisDeUsado() {
        CodigoVerificacao codigo = codigoSalvo("123456", agora.minusMinutes(1), 0);
        when(codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(EMAIL, LOGIN)).thenReturn(Optional.of(codigo));

        service.validarCodigo(EMAIL, LOGIN, "123456");

        verify(codigoRepository).delete(codigo);
    }

    @Test
    void codigoErradoContaUmaTentativa() {
        CodigoVerificacao codigo = codigoSalvo("123456", agora.minusMinutes(1), 0);
        when(codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(EMAIL, LOGIN)).thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> service.validarCodigo(EMAIL, LOGIN, "000000"))
                .isInstanceOf(CodigoInvalidoException.class);
        assertThat(codigo.getTentativas()).isEqualTo(1);
        verify(codigoRepository).save(codigo);
    }

    @Test
    void codigoExpiradoNaoVale() {
        CodigoVerificacao codigo = codigoSalvo("123456", agora.minusMinutes(11), 0);
        when(codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(EMAIL, LOGIN)).thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> service.validarCodigo(EMAIL, LOGIN, "123456"))
                .isInstanceOf(CodigoInvalidoException.class);
        verify(codigoRepository).delete(codigo);
    }

    @Test
    void depoisDeCincoTentativasNemOCodigoCertoVale() {
        CodigoVerificacao codigo = codigoSalvo("123456", agora.minusMinutes(1), CodigoVerificacaoService.MAXIMO_TENTATIVAS);
        when(codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(EMAIL, LOGIN)).thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> service.validarCodigo(EMAIL, LOGIN, "123456"))
                .isInstanceOf(CodigoInvalidoException.class);
    }

    @Test
    void semCodigoPedidoAValidacaoFalha() {
        when(codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(EMAIL, LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarCodigo(EMAIL, LOGIN, "123456"))
                .isInstanceOf(CodigoInvalidoException.class);
    }
}
