package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.UsuarioService;
import com.backend.hippo_api.business.dtos.in.LoginDTORequest;
import com.backend.hippo_api.business.dtos.in.UsuarioCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.UsuarioBuscaDadosDTOResponse;
import com.backend.hippo_api.business.dtos.out.UsuarioCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.security.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<UsuarioCadastroDTOResponse> cadastrarUsuario(@Valid @RequestBody UsuarioCadastroDTORequest usuarioDTO) {
        return ResponseEntity.ok(usuarioService.cadastrarUsuario(usuarioDTO));
    }

    @PostMapping("/login")
    public String loginUsuario(@RequestBody LoginDTORequest loginDTORequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTORequest.getEmail(), loginDTORequest.getSenha())
        );
        return "Bearer " + jwtUtil.generateToken(authentication.getName());
    }

    @GetMapping
    public ResponseEntity<UsuarioBuscaDadosDTOResponse> buscarUsuarioPorId(@RequestParam("id") Long id) {
        return ResponseEntity.ok(usuarioService.buscarUsuarioPorId(id));
    }
}
