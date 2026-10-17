package com.backend.hippo_api.business;

import com.backend.hippo_api.business.converter.TarefaConverter;
import com.backend.hippo_api.business.dtos.in.TarefaAlterarDadosDTORequest;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.*;
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
    public List<TarefaBuscaDadosDTOResponse> listarTarefas(Usuario usuario) {
        return tarefaConverter.converterParaListaTarefaDTOBuscar(
                tarefaRepository.findByUsuario(usuario)
        );
    }

    // Método para buscar uma Tarefa por ID
    public TarefaBuscaPorIdDTOResponse buscarTarefaPorId(Long id, Usuario usuario) {
        return tarefaConverter.converterParaTarefaDTOBuscarPorId(
                tarefaRepository.findByIdAndUsuario(id, usuario).orElseThrow(
                        () -> new ResourceNotFoundException("Erro: Tarefa de ID " + id + " não Encontrada!")
                )
        );
    }

    // Método para deletar uma Tarefa por ID
    public void deletarTarefa(Long id, Usuario usuario) {
        // Encontrar a Tarefa à ser deletada
        Tarefa tarefa = tarefaRepository.findByIdAndUsuario(id, usuario).orElseThrow(
                () -> new ResourceNotFoundException("Erro: Tarefa de ID " + id + " não Encontrada!")
        );

        // Deletar a Tarefa
        tarefaRepository.delete(tarefa);
    }

    // Método para alterar o Status de uma Tarefa por ID
    public TarefaAlterarStatusDTOResponse alterarStatusTarefa(Long id, StatusTarefaEnum status, Usuario usuario) {
        try {
            // Encontrar a Tarefa cujo Status será alterado
            Tarefa tarefa = tarefaRepository.findByIdAndUsuario(id, usuario).orElseThrow(
                    () -> new ResourceNotFoundException("Erro: Tarefa de ID " + id + " não Encontrada!")
            );

            // Alterar o Status da Tarefa
            tarefa.setStatusTarefa(status);

            // Salvar a alteração do Status
            return tarefaConverter.converterParaTarefaDTOAlterarStatus(tarefaRepository.save(tarefa));
        }
        catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Erro: Tarefa de ID " + id + " não Encontrada!");
        }
    }

    // Método para alterar Dados de uma Tarefa por ID
    public TarefaAlterarDadosDTOResponse alterarDadosTarefa(Long id,
                                                            TarefaAlterarDadosDTORequest tarefaDTO,
                                                            Usuario usuario) {
        try {
            // Encontrar a Tarefa à ser alterada
            Tarefa tarefa = tarefaRepository.findByIdAndUsuario(id, usuario).orElseThrow(
                    () -> new ResourceNotFoundException("Erro: Tarefa de ID " + id + " não Encontrada!")
            );

            // Atualizar os Dados da Tarefa
            Tarefa tarefaAlterada = tarefaConverter.atualizarTarefa(tarefaDTO, tarefa);

            // Salvar a alteração dos Dados
            return tarefaConverter.converterParaTarefaDTOAlterarDados(tarefaRepository.save(tarefaAlterada));
        }
        catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Erro: Tarefa de ID " + id + " não Encontrada!");
        }
    }
}
