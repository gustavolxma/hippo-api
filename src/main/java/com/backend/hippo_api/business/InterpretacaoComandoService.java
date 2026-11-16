package com.backend.hippo_api.business;

import com.backend.hippo_api.business.dtos.in.InterpretarComandoDTORequest;
import com.backend.hippo_api.business.dtos.out.InterpretacaoComandoDTOResponse;
import com.backend.hippo_api.infrastructure.enums.PrioridadeTarefaEnum;
import com.backend.hippo_api.infrastructure.exceptions.ServicoExternoException;
import com.backend.hippo_api.infrastructure.ia.GeminiClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Interpreta a frase falada pelo Usuário (Processamento de Linguagem Natural com o Gemini)
// e devolve os dados da Tarefa. A resposta da IA é sempre conferida antes de chegar ao app.
@Slf4j
@Service
@RequiredArgsConstructor
public class InterpretacaoComandoService {

    // Lembretes que o app oferece (minutos antes do horário)
    static final Set<Integer> LEMBRETES_VALIDOS = Set.of(0, 10, 30, 60, 1440);

    private static final DateTimeFormatter FORMATO_AGORA =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private static final String INSTRUCOES = """
            Você é o Hippo, um assistente que transforma frases em português do Brasil em tarefas.
            Responda SOMENTE com o JSON pedido.

            Regras:
            - "entendeu": false se a frase não pede para criar uma tarefa ou lembrete (ex.: cumprimentos,
              perguntas soltas, frases sem uma ação). Nesse caso deixe os outros campos vazios.
            - "nomeTarefa": a ação, curta, começando com verbo e letra maiúscula, sem a data, a hora
              e a prioridade. Ex.: "Me lembra de enviar o relatório amanhã" -> "Enviar o relatório".
            - "descricao": detalhes extras da frase que não couberam no nome; senão, vazio.
            - "data": no formato AAAA-MM-DD, calculada a partir do momento atual informado
              ("hoje", "amanhã", "sexta", "dia 20"...). Dia da semana citado = a próxima ocorrência.
              Vazio se a frase não indica dia.
            - "hora": no formato HH:MM (24 horas). "3 da tarde" = 15:00, "meio-dia" = 12:00. Vazio se não houver.
            - "prioridade": ALTA para "urgente", "importante" ou "prioridade alta"; BAIXA para "sem pressa"
              ou "prioridade baixa"; MEDIA nos demais casos.
            - "lembreteMinutosAntes": só se a pessoa pedir lembrete/aviso ("me lembra", "me avisa").
              Use 0 para "na hora", ou os minutos pedidos ("30 minutos antes" = 30, "1 dia antes" = 1440).
              Se pediu lembrete sem dizer quando, use 0. Nulo se não pediu.
            """;

    // Esquema da resposta (subconjunto de OpenAPI aceito pelo Gemini)
    private static final Map<String, Object> ESQUEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "entendeu", Map.of("type", "BOOLEAN"),
                    "nomeTarefa", Map.of("type", "STRING"),
                    "descricao", Map.of("type", "STRING"),
                    "data", Map.of("type", "STRING", "description", "AAAA-MM-DD ou vazio"),
                    "hora", Map.of("type", "STRING", "description", "HH:MM ou vazio"),
                    "prioridade", Map.of("type", "STRING", "enum", List.of("BAIXA", "MEDIA", "ALTA")),
                    "lembreteMinutosAntes", Map.of("type", "INTEGER", "nullable", true)
            ),
            "required", List.of("entendeu", "nomeTarefa", "prioridade")
    );

    private final GeminiClient geminiClient;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public InterpretacaoComandoDTOResponse interpretar(InterpretarComandoDTORequest comandoDTO) {
        LocalDateTime agora = comandoDTO.getAgora() != null ? comandoDTO.getAgora() : LocalDateTime.now(clock);
        String texto = "Momento atual: " + FORMATO_AGORA.format(agora) + "\nFrase: " + comandoDTO.getTexto().trim();

        String json = geminiClient.gerarJson(INSTRUCOES, texto, ESQUEMA);
        RespostaIa resposta;
        try {
            resposta = jsonMapper.readValue(json, RespostaIa.class);
        }
        catch (JacksonException e) {
            log.error("Resposta da IA fora do formato esperado: {}", json, e);
            throw new ServicoExternoException("Não foi possível interpretar o comando agora.", e);
        }

        return conferir(resposta, agora);
    }

    // A IA pode errar o formato: tudo é conferido aqui antes de ir para o app
    private InterpretacaoComandoDTOResponse conferir(RespostaIa resposta, LocalDateTime agora) {
        String nomeTarefa = textoOuNulo(resposta.nomeTarefa());
        if (!resposta.entendeu() || nomeTarefa == null) {
            return InterpretacaoComandoDTOResponse.builder().entendeu(false).build();
        }

        LocalDate data = converterData(resposta.data());
        LocalTime hora = converterHora(resposta.hora());
        // "às 15h" sem dia: hoje, se ainda não passou; senão, amanhã (mesma regra do app)
        if (hora != null && data == null) {
            data = hora.isAfter(agora.toLocalTime()) ? agora.toLocalDate() : agora.toLocalDate().plusDays(1);
        }

        // Lembrete só faz sentido com horário e com um dos valores que o app oferece
        Integer lembrete = resposta.lembreteMinutosAntes();
        if (hora == null || lembrete == null || !LEMBRETES_VALIDOS.contains(lembrete)) {
            lembrete = null;
        }

        return InterpretacaoComandoDTOResponse.builder()
                .entendeu(true)
                .nomeTarefa(nomeTarefa)
                .descricao(textoOuNulo(resposta.descricao()))
                .data(data)
                .hora(hora)
                .prioridade(converterPrioridade(resposta.prioridade()))
                .lembreteMinutosAntes(lembrete)
                .build();
    }

    private LocalDate converterData(String data) {
        try {
            return textoOuNulo(data) == null ? null : LocalDate.parse(data.trim());
        }
        catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalTime converterHora(String hora) {
        try {
            return textoOuNulo(hora) == null ? null : LocalTime.parse(hora.trim());
        }
        catch (DateTimeParseException e) {
            return null;
        }
    }

    private PrioridadeTarefaEnum converterPrioridade(String prioridade) {
        try {
            return prioridade == null ? PrioridadeTarefaEnum.MEDIA : PrioridadeTarefaEnum.valueOf(prioridade.trim().toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return PrioridadeTarefaEnum.MEDIA;
        }
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    // Formato do JSON que a IA devolve (definido no ESQUEMA)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaIa(boolean entendeu, String nomeTarefa, String descricao, String data, String hora,
                      String prioridade, Integer lembreteMinutosAntes) {
    }
}
