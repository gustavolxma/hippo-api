package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.InterpretacaoComandoService;
import com.backend.hippo_api.business.dtos.in.InterpretarComandoDTORequest;
import com.backend.hippo_api.business.dtos.out.InterpretacaoComandoDTOResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ia")
@RequiredArgsConstructor
public class IaController {

    private final InterpretacaoComandoService interpretacaoComandoService;

    // Interpreta a frase falada no app (exige estar logado, para a cota gratuita da IA não ser usada por qualquer um)
    @PostMapping("/interpretar")
    public ResponseEntity<InterpretacaoComandoDTOResponse> interpretarComando(@Valid @RequestBody InterpretarComandoDTORequest comandoDTO) {
        return ResponseEntity.ok(interpretacaoComandoService.interpretar(comandoDTO));
    }
}
