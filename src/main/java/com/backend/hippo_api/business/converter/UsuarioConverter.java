package com.backend.hippo_api.business.converter;

import com.backend.hippo_api.business.dtos.in.UsuarioCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.UsuarioBuscaDadosDTOResponse;
import com.backend.hippo_api.business.dtos.out.UsuarioCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Usuario;

import org.springframework.stereotype.Component;

@Component
public class UsuarioConverter {
    // Converter de UsuarioDTO para Usuario - Cadastro
    public Usuario converterParaUsuarioCadastro(UsuarioCadastroDTORequest usuarioDTO) {
        return Usuario.builder()
                .nome(usuarioDTO.getNome())
                .email(usuarioDTO.getEmail())
                .senha(usuarioDTO.getSenha())
                .build();
    }

    // Converter de Usuario para UsuarioDTO - Cadastro
    public UsuarioCadastroDTOResponse converterParaUsuarioDTOCadastro(Usuario usuario) {
        return UsuarioCadastroDTOResponse.builder()
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .dataCadastro(usuario.getDataCadastro())
                .build();
    }

    // Converter para um Usuario para UsuarioDTO - Buscar Dados
    public UsuarioBuscaDadosDTOResponse converterParaUsuarioDTOBuscarDados(Usuario usuario) {
        return UsuarioBuscaDadosDTOResponse.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .dataCadastro(usuario.getDataCadastro())
                .build();
    }
}
