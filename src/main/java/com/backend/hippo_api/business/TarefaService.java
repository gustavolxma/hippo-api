package com.backend.hippo_api.business;

import com.backend.hippo_api.business.converter.TarefaConverter;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.TarefaBuscaDadosDTOResponse;
import com.backend.hippo_api.business.dtos.out.TarefaCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Tarefa;
import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.enums.StatusTarefaEnum;
import com.backend.hippo_api.infrastructure.exceptions.ResourceNotFoundException;
import com.backend.hippo_api.infrastructure.repository.TarefaRepository;
import com.backend.hippo_api.infrastructure.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final TarefaConverter tarefaConverter;
    private final UsuarioRepository usuarioRepository;

    // Método para cadastrar Tarefas de um Usuário
    public TarefaCadastroDTOResponse cadastrarTarefa(TarefaCadastroDTORequest tarefaDTO, String email) {
        // Pegar o Usuário responsável pela Tarefa através do Email
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Erro: Usuário de Email " + email + " não Encontrado!")
        );

        // Transformar a TarefaDTO em Tarafa
        Tarefa tarefa = tarefaConverter.converterParaTarefaCadastro(tarefaDTO);

        // Setar o Status da Tarefa como PENDENTE e o Horário da Criação (atual) e ID do Usuário
        tarefa.setStatusTarefa(StatusTarefaEnum.PENDENTE);
        tarefa.setDataCriacao(LocalDateTime.now());
        tarefa.setUsuario(usuario);

        // Salvar no Banco de Dados
        return tarefaConverter.converterParaTarefaDTOCadastro(
                tarefaRepository.save(tarefa)
        );
    }

    // Método para buscar todas as Tarefas que estão armazenadas
    public List<TarefaBuscaDadosDTOResponse> listarTodasTarefas() {
        return tarefaConverter.converterParaListaTarefaDTOBuscar(
                tarefaRepository.findAll()
        );
    }

    // Método para buscar todas as Tarefas de um determinado Usuário
    public List<TarefaBuscaDadosDTOResponse> listarTarefasUsuario(Long idUsuario) {
        // Pegar o Usuário através do ID
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(
                () -> new ResourceNotFoundException("Usuario de ID " + idUsuario + " não Encontrado!")
        );

        // Buscar e Retornar as Tarefas do Usuário
        return tarefaConverter.converterParaListaTarefaDTOBuscar(
            tarefaRepository.findByUsuario(usuario).orElseThrow(
                    () -> new ResourceNotFoundException("Erro: Usuário de ID " + idUsuario + " não Encontrado!")
            )
        );
    }
}
