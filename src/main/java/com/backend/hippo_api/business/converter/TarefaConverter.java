package com.backend.hippo_api.business.converter;

import com.backend.hippo_api.business.dtos.in.TarefaAlterarDadosDTORequest;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.*;
import com.backend.hippo_api.infrastructure.entity.Tarefa;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TarefaConverter {
    // Converter de TarefaDTO para Tarefa - Cadastro
    public Tarefa converterParaTarefaCadastro(TarefaCadastroDTORequest tarefaDTO) {
        return Tarefa.builder()
                .nomeTarefa(tarefaDTO.getNomeTarefa())
                .descricao(tarefaDTO.getDescricao())
                .dataEvento(tarefaDTO.getDataEvento())
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
                .build();
    }



    // Converter de Tarefa para TarefaDTO - Alterar Status
    public TarefaAlterarStatusDTOResponse converterParaTarefaDTOAlterarStatus(Tarefa tarefa) {
        return TarefaAlterarStatusDTOResponse.builder()
                .nomeTarefa(tarefa.getNomeTarefa())
                .descricao(tarefa.getDescricao())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataEvento(tarefa.getDataEvento())
                .build();
    }



    // Atualizar os Dados de um Tarefa - Atualizar Dados
    public Tarefa atualizarTarefa(TarefaAlterarDadosDTORequest tarefaDTO, Tarefa tarefa) {
        return Tarefa.builder()
                .id(tarefa.getId())
                .nomeTarefa(tarefaDTO.getNomeTarefa() != null ? tarefaDTO.getNomeTarefa() : tarefa.getNomeTarefa())
                .descricao(tarefaDTO.getDescricao() != null ? tarefaDTO.getDescricao() : tarefa.getDescricao())
                .dataEvento(tarefaDTO.getDataEvento() != null ? tarefaDTO.getDataEvento() : tarefa.getDataEvento())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataCriacao(tarefa.getDataCriacao())
                .usuario(tarefa.getUsuario())
                .build();
    }

    // Converter de Tarefa para TarefaDTO - Alterar Dados
    public TarefaAlterarDadosDTOResponse converterParaTarefaDTOAlterarDados(Tarefa tarefa) {
        return TarefaAlterarDadosDTOResponse.builder()
                .nomeTarefa(tarefa.getNomeTarefa())
                .descricao(tarefa.getDescricao())
                .statusTarefa(tarefa.getStatusTarefa())
                .dataCriacao(tarefa.getDataCriacao())
                .dataEvento(tarefa.getDataEvento())
                .build();
    }
}
