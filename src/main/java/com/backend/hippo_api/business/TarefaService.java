package com.backend.hippo_api.business;

import com.backend.hippo_api.business.converter.TarefaConverter;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.TarefaCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Tarefa;
import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.enums.StatusTarefaEnum;
import com.backend.hippo_api.infrastructure.exceptions.ResourceNotFoundException;
import com.backend.hippo_api.infrastructure.repository.TarefaRepository;

import com.backend.hippo_api.infrastructure.repository.UsuarioRepository;
import com.backend.hippo_api.infrastructure.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final TarefaConverter tarefaConverter;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public TarefaCadastroDTOResponse cadastrarTarefa(TarefaCadastroDTORequest tarefaDTO, String token) {
        // Pegar o Usuário responsável pela Tarefa através do Email extraído do Token
        String email = jwtUtil.extractUsername(token.substring(7));
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Erro: Usuário de Email " + email + " não Encontrado!")
        );

        // Transformar a TarefaDTO em Tarafa
        Tarefa tarefa = tarefaConverter.converterParaTarefaCadastro(tarefaDTO);

        // Setar o Status da Tarefa como PENDENTE e o Horário da Criação (atual) e ID do Usuário
        tarefa.setStatusTarefa(StatusTarefaEnum.PENDENTE);
        tarefa.setDataCriacao(LocalDateTime.now());
        tarefa.setIdUsuario(usuario.getId());

        // Salvar no Banco de Dados
        return tarefaConverter.converterParaTarefaDTOCadastro(
                tarefaRepository.save(tarefa)
        );
    }
}
