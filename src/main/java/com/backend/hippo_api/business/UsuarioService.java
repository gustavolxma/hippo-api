package com.backend.hippo_api.business;

import com.backend.hippo_api.business.converter.UsuarioConverter;
import com.backend.hippo_api.business.dtos.in.UsuarioCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.UsuarioBuscaDadosDTOResponse;
import com.backend.hippo_api.business.dtos.out.UsuarioCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.exceptions.ConflictException;
import com.backend.hippo_api.infrastructure.exceptions.ResourceNotFoundException;
import com.backend.hippo_api.infrastructure.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioConverter usuarioConverter;
    private final PasswordEncoder passwordEncoder;

    public UsuarioCadastroDTOResponse cadastrarUsuario(UsuarioCadastroDTORequest usuarioDTO) {
        // Verficiar se o Email já existe no Banco de Dados
        emailExiste(usuarioDTO.getEmail());

        // Transformar o UsuarioDTO em Usuario
        Usuario usuario = usuarioConverter.converterParaUsuarioCadastro(usuarioDTO);

        // Criptografar a Senha e setar a Data do Cadastro
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setDataCadastro(LocalDateTime.now());

        // Salvar no Banco de Dados
        return usuarioConverter.converterParaUsuarioDTOCadastro(
                usuarioRepository.save(usuario)
        );
    }

    private void emailExiste(String email) {
        boolean emailExiste = verificarSeEmailExiste(email);

        if (emailExiste) {
            throw new ConflictException("Erro: Email " + email + " já existe!");
        }
    }

    private boolean verificarSeEmailExiste(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public UsuarioBuscaDadosDTOResponse buscarUsuarioPorId(Long id) {
        return usuarioConverter.converterParaUsuarioDTOBuscarDados(
                usuarioRepository.findById(id).orElseThrow(
                        () -> new ResourceNotFoundException("Erro: Usuário de ID " + id + " não Encontrado!")
                )
        );
    }
}
