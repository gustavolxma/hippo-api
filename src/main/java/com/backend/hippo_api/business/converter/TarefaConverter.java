package com.backend.hippo_api.business.converter;

import com.backend.hippo_api.business.dtos.in.TarefaAlterarDadosDTORequest;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.*;
import com.backend.hippo_api.infrastructure.entity.Tarefa;
import com.backend.hippo_api.infrastructure.enums.PrioridadeTarefaEnum;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TarefaConverter {
    // Converter de TarefaDTO para Tarefa - Cadastro
    public Tarefa converterParaTarefaCadastro(TarefaCadastroDTORequest tarefaDTO) {
        return Tarefa.builder()
                .nomeTarefa(tarefaDTO.getNomeTarefa())
                .descricao(textoOuVazio(tarefaDTO.getDescricao()))
                .dataEvento(tarefaDTO.getDataEvento())
                .temHora(temHora(tarefaDTO.getDataEvento(), tarefaDTO.getTemHora()))
                .prioridade(prioridadeOuMedia(tarefaDTO.getPrioridade()))
                .lembreteMinutosAntes(tarefaDTO.getLembreteMinutosAntes())
                .build();
    }

    // Converter de Tarefa para TarefaDTO - Cadastro
    public TarefaCadastroDTOResponse converterParaTarefaDTOCadastro(Tarefa tarefa) {
        return TarefaCadastroDTOResponse.builder()
                .id(tarefa.getId())
                .nomeTarefa(tarefa.getNomeTarefa())
                .descricao(tarefa.getDescricao())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataCriacao(tarefa.getDataCriacao())
                .dataEvento(tarefa.getDataEvento())
                .temHora(temHora(tarefa))
                .prioridade(prioridadeOuMedia(tarefa.getPrioridade()))
                .lembreteMinutosAntes(tarefa.getLembreteMinutosAntes())
                .build();
    }



    // Converter de Tarefa para TarefaDTO - Buscar todas as Tarefas de um Usuário
    private TarefaBuscaDadosDTOResponse converterParaTarefaDTOBuscar(Tarefa tarefa) {
        return TarefaBuscaDadosDTOResponse.builder()
                .id(tarefa.getId())
                .nomeTarefa(tarefa.getNomeTarefa())
                .descricao(tarefa.getDescricao())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataCriacao(tarefa.getDataCriacao())
                .dataEvento(tarefa.getDataEvento())
                .temHora(temHora(tarefa))
                .prioridade(prioridadeOuMedia(tarefa.getPrioridade()))
                .lembreteMinutosAntes(tarefa.getLembreteMinutosAntes())
                .idUsuario(tarefa.getUsuario().getId())
                .build();
    }

    // Converter uma Lista de Tarefa para uma Lista de TarefaDTO - Buscar todas as Tarefas de um Usuário
    public List<TarefaBuscaDadosDTOResponse> converterParaListaTarefaDTOBuscar(List<Tarefa> listaTarefa) {
        return listaTarefa.stream().map(this::converterParaTarefaDTOBuscar).toList();
    }



    // Converter de Tarefa para TarefaDTO - Buscar uma Tarefa por ID
    public TarefaBuscaPorIdDTOResponse converterParaTarefaDTOBuscarPorId(Tarefa tarefa) {
        return TarefaBuscaPorIdDTOResponse.builder()
                .nomeTarefa(tarefa.getNomeTarefa())
                .descricao(tarefa.getDescricao())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataCriacao(tarefa.getDataCriacao())
                .dataEvento(tarefa.getDataEvento())
                .temHora(temHora(tarefa))
                .prioridade(prioridadeOuMedia(tarefa.getPrioridade()))
                .lembreteMinutosAntes(tarefa.getLembreteMinutosAntes())
                .build();
    }



    // Converter de Tarefa para TarefaDTO - Alterar Status
    public TarefaAlterarStatusDTOResponse converterParaTarefaDTOAlterarStatus(Tarefa tarefa) {
        return TarefaAlterarStatusDTOResponse.builder()
                .nomeTarefa(tarefa.getNomeTarefa())
                .descricao(tarefa.getDescricao())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataEvento(tarefa.getDataEvento())
                .temHora(temHora(tarefa))
                .prioridade(prioridadeOuMedia(tarefa.getPrioridade()))
                .lembreteMinutosAntes(tarefa.getLembreteMinutosAntes())
                .build();
    }



    // Atualizar os Dados de um Tarefa - Atualizar Dados
    public void atualizarTarefa(TarefaAlterarDadosDTORequest tarefaDTO, Tarefa tarefa) {
        tarefa.setNomeTarefa(tarefaDTO.getNomeTarefa());
        tarefa.setDescricao(textoOuVazio(tarefaDTO.getDescricao()));
        tarefa.setDataEvento(tarefaDTO.getDataEvento());
        tarefa.setTemHora(temHora(tarefaDTO.getDataEvento(), tarefaDTO.getTemHora()));
        tarefa.setPrioridade(prioridadeOuMedia(tarefaDTO.getPrioridade()));
        tarefa.setLembreteMinutosAntes(tarefaDTO.getLembreteMinutosAntes());
    }

    // Converter de Tarefa para TarefaDTO - Alterar Dados
    public TarefaAlterarDadosDTOResponse converterParaTarefaDTOAlterarDados(Tarefa tarefa) {
        return TarefaAlterarDadosDTOResponse.builder()
                .nomeTarefa(tarefa.getNomeTarefa())
                .descricao(tarefa.getDescricao())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataCriacao(tarefa.getDataCriacao())
                .dataEvento(tarefa.getDataEvento())
                .temHora(temHora(tarefa))
                .prioridade(prioridadeOuMedia(tarefa.getPrioridade()))
                .lembreteMinutosAntes(tarefa.getLembreteMinutosAntes())
                .build();
    }

    private String textoOuVazio(String texto) {
        return texto == null ? "" : texto.trim();
    }

    // Tarefas criadas antes desses campos existirem não têm prioridade nem temHora salvos
    private PrioridadeTarefaEnum prioridadeOuMedia(PrioridadeTarefaEnum prioridade) {
        return prioridade == null ? PrioridadeTarefaEnum.MEDIA : prioridade;
    }

    private boolean temHora(Tarefa tarefa) {
        return temHora(tarefa.getDataEvento(), tarefa.getTemHora());
    }

    private boolean temHora(LocalDateTime dataEvento, Boolean temHora) {
        return dataEvento != null && !Boolean.FALSE.equals(temHora);
    }
}
