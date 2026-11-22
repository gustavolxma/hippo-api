package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.UsuarioService;
import com.backend.hippo_api.business.dtos.in.LoginDTORequest;
import com.backend.hippo_api.business.dtos.in.ReenviarCodigoDTORequest;
import com.backend.hippo_api.business.dtos.in.UsuarioAlterarDadosDTORequest;
import com.backend.hippo_api.business.dtos.in.UsuarioCadastroDTORequest;
import com.backend.hippo_api.business.dtos.in.VerificarCodigoDTORequest;
import com.backend.hippo_api.business.dtos.out.MensagemDTOResponse;
import com.backend.hippo_api.business.dtos.out.TokenDTOResponse;
import com.backend.hippo_api.business.dtos.out.UsuarioBuscaDadosDTOResponse;
import com.backend.hippo_api.business.dtos.out.UsuarioCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Usuario;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;

    // Cadastro em duas etapas: cria a conta e envia um Código para o Email (confirmado em /verificar-cadastro)
    @PostMapping
    public ResponseEntity<UsuarioCadastroDTOResponse> cadastrarUsuario(@Valid @RequestBody UsuarioCadastroDTORequest usuarioDTO) {
        UsuarioCadastroDTOResponse usuario = usuarioService.cadastrarUsuario(usuarioDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }

    @PostMapping("/verificar-cadastro")
    public ResponseEntity<TokenDTOResponse> verificarCadastro(@Valid @RequestBody VerificarCodigoDTORequest verificarDTO) {
        return ResponseEntity.ok(usuarioService.verificarCadastro(verificarDTO));
    }

    // Login em duas etapas: confere Email e Senha e envia um Código (o Token só sai em /login/verificar)
    @PostMapping("/login")
    public ResponseEntity<MensagemDTOResponse> loginUsuario(@RequestBody LoginDTORequest loginDTORequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTORequest.getEmail(), loginDTORequest.getSenha())
        );
        usuarioService.enviarCodigoLogin(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new MensagemDTOResponse("Código de acesso enviado para o Email!"));
    }

    @PostMapping("/login/verificar")
    public ResponseEntity<TokenDTOResponse> verificarLogin(@Valid @RequestBody VerificarCodigoDTORequest verificarDTO) {
        return ResponseEntity.ok(usuarioService.verificarLogin(verificarDTO));
    }

    @PostMapping("/codigo/reenviar")
    public ResponseEntity<MensagemDTOResponse> reenviarCodigo(@Valid @RequestBody ReenviarCodigoDTORequest reenviarDTO) {
        usuarioService.reenviarCodigo(reenviarDTO);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new MensagemDTOResponse("Novo Código enviado para o Email!"));
    }

    // Alterar Email ou Senha em duas etapas (autenticado): pede o Código e depois envia o PUT com ele
    @PostMapping("/conta/codigo")
    public ResponseEntity<MensagemDTOResponse> enviarCodigoAlterarConta(@AuthenticationPrincipal Usuario usuario) {
        usuarioService.enviarCodigoAlterarConta(usuario);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new MensagemDTOResponse("Código enviado para o Email atual!"));
    }

    @PutMapping
    public ResponseEntity<TokenDTOResponse> alterarDadosUsuario(@Valid @RequestBody UsuarioAlterarDadosDTORequest alterarDTO,
                                                                @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioService.alterarDadosUsuario(usuario, alterarDTO));
    }

    @GetMapping
    public ResponseEntity<UsuarioBuscaDadosDTOResponse> buscarUsuarioPorId(@RequestParam("id") Long id) {
        return ResponseEntity.ok(usuarioService.buscarUsuarioPorId(id));
    }
}
