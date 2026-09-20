package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.TarefaService;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.TarefaCadastroDTOResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;

    @PostMapping
    public ResponseEntity<TarefaCadastroDTOResponse> cadastrarTarefa(@RequestBody TarefaCadastroDTORequest tarefaDTO,
                                                                     @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(tarefaService.cadastrarTarefa(tarefaDTO, token));
    }
}
