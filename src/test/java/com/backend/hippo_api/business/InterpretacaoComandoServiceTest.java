package com.backend.hippo_api.business;

import com.backend.hippo_api.business.dtos.in.InterpretarComandoDTORequest;
import com.backend.hippo_api.business.dtos.out.InterpretacaoComandoDTOResponse;
import com.backend.hippo_api.infrastructure.enums.PrioridadeTarefaEnum;
import com.backend.hippo_api.infrastructure.exceptions.ServicoExternoException;
import com.backend.hippo_api.infrastructure.ia.GeminiClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterpretacaoComandoServiceTest {

    @Mock
    private GeminiClient geminiClient;

    private InterpretacaoComandoService service;

    // Momento da fala no celular: segunda-feira, 16/11/2026, 9h
    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 16, 9, 0);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneId.of("UTC"));
        service = new InterpretacaoComandoService(geminiClient, JsonMapper.builder().build(), clock);
    }

    private InterpretacaoComandoDTOResponse interpretarComRespostaDaIa(String json) {
        when(geminiClient.gerarJson(anyString(), anyString(), any())).thenReturn(json);
        return service.interpretar(new InterpretarComandoDTORequest("frase", agora));
    }

    @Test
    void respostaCompletaDaIaChegaAoApp() {
        InterpretacaoComandoDTOResponse resposta = interpretarComRespostaDaIa("""
                {"entendeu": true, "nomeTarefa": "Enviar o relatório", "descricao": "",
                 "data": "2026-11-17", "hora": "15:00", "prioridade": "ALTA", "lembreteMinutosAntes": 0}
                """);

        assertThat(resposta.isEntendeu()).isTrue();
        assertThat(resposta.getNomeTarefa()).isEqualTo("Enviar o relatório");
        assertThat(resposta.getDescricao()).isNull();
        assertThat(resposta.getData()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(resposta.getHora()).isEqualTo(LocalTime.of(15, 0));
        assertThat(resposta.getPrioridade()).isEqualTo(PrioridadeTarefaEnum.ALTA);
        assertThat(resposta.getLembreteMinutosAntes()).isZero();
    }

    @Test
    void enviaAoGeminiOMomentoDoCelularComODiaDaSemana() {
        interpretarComRespostaDaIa("{\"entendeu\": false, \"nomeTarefa\": \"\", \"prioridade\": \"MEDIA\"}");

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).gerarJson(anyString(), texto.capture(), any());
        assertThat(texto.getValue()).contains("segunda-feira, 16/11/2026 às 09:00").contains("Frase: frase");
    }

    @Test
    void horaSemDataViraHojeOuAmanha() {
        InterpretacaoComandoDTOResponse maisTarde = interpretarComRespostaDaIa("""
                {"entendeu": true, "nomeTarefa": "Ligar", "data": "", "hora": "14:00", "prioridade": "MEDIA"}
                """);
        assertThat(maisTarde.getData()).isEqualTo(LocalDate.of(2026, 11, 16));

        InterpretacaoComandoDTOResponse jaPassou = interpretarComRespostaDaIa("""
                {"entendeu": true, "nomeTarefa": "Ligar", "data": "", "hora": "08:00", "prioridade": "MEDIA"}
                """);
        assertThat(jaPassou.getData()).isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test
    void lembreteSemHorarioOuForaDasOpcoesEhDescartado() {
        InterpretacaoComandoDTOResponse semHora = interpretarComRespostaDaIa("""
                {"entendeu": true, "nomeTarefa": "Estudar", "data": "2026-11-17", "hora": "",
                 "prioridade": "MEDIA", "lembreteMinutosAntes": 30}
                """);
        assertThat(semHora.getLembreteMinutosAntes()).isNull();

        InterpretacaoComandoDTOResponse foraDasOpcoes = interpretarComRespostaDaIa("""
                {"entendeu": true, "nomeTarefa": "Estudar", "data": "2026-11-17", "hora": "10:00",
                 "prioridade": "MEDIA", "lembreteMinutosAntes": 45}
                """);
        assertThat(foraDasOpcoes.getLembreteMinutosAntes()).isNull();
    }

    @Test
    void camposEmFormatoErradoSaoCorrigidos() {
        InterpretacaoComandoDTOResponse resposta = interpretarComRespostaDaIa("""
                {"entendeu": true, "nomeTarefa": "  Pagar a conta ", "data": "amanhã", "hora": "15h",
                 "prioridade": "URGENTISSIMA"}
                """);

        assertThat(resposta.getNomeTarefa()).isEqualTo("Pagar a conta");
        assertThat(resposta.getData()).isNull();
        assertThat(resposta.getHora()).isNull();
        assertThat(resposta.getPrioridade()).isEqualTo(PrioridadeTarefaEnum.MEDIA);
    }

    @Test
    void naoEntendeuOuSemNomeVoltaSoComEntendeuFalso() {
        assertThat(interpretarComRespostaDaIa("""
                {"entendeu": false, "nomeTarefa": "", "prioridade": "MEDIA"}
                """).isEntendeu()).isFalse();

        InterpretacaoComandoDTOResponse semNome = interpretarComRespostaDaIa("""
                {"entendeu": true, "nomeTarefa": "  ", "data": "2026-11-17", "prioridade": "ALTA"}
                """);
        assertThat(semNome.isEntendeu()).isFalse();
        assertThat(semNome.getData()).isNull();
    }

    @Test
    void jsonInvalidoDaIaViraServicoIndisponivel() {
        assertThatThrownBy(() -> interpretarComRespostaDaIa("isso não é json"))
                .isInstanceOf(ServicoExternoException.class);
    }

    @Test
    void semMomentoDoCelularUsaOHorarioDoServidor() {
        when(geminiClient.gerarJson(anyString(), anyString(), any()))
                .thenReturn("{\"entendeu\": false, \"nomeTarefa\": \"\", \"prioridade\": \"MEDIA\"}");

        service.interpretar(new InterpretarComandoDTORequest("frase", null));

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).gerarJson(anyString(), texto.capture(), any());
        assertThat(texto.getValue()).contains("16/11/2026 às 12:00");
    }
}
