package com.backend.hippo_api.business.converter;

import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.TarefaCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Tarefa;

import org.springframework.stereotype.Component;

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
}
