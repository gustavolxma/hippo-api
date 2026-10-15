package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.TarefaService;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.TarefaBuscaDadosDTOResponse;
import com.backend.hippo_api.business.dtos.out.TarefaCadastroDTOResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;

    @PostMapping
    public ResponseEntity<TarefaCadastroDTOResponse> cadastrarTarefa(@Valid @RequestBody TarefaCadastroDTORequest tarefaDTO,
                                                                     @AuthenticationPrincipal UserDetails userDetails) {
         TarefaCadastroDTOResponse tarefa = tarefaService.cadastrarTarefa(tarefaDTO, userDetails.getUsername());

         URI location = URI.create("/tarefas/" + tarefa.getId());

        return ResponseEntity
                .created(location)
                .body(tarefa);
    }

    @GetMapping
    public ResponseEntity<List<TarefaBuscaDadosDTOResponse>> listarTarefas() {
        return ResponseEntity.ok(tarefaService.listarTodasTarefas());
    }

    @GetMapping("/usuario")
    public ResponseEntity<List<TarefaBuscaDadosDTOResponse>> listarTarefasUsuario(@RequestParam("idUsuario") Long idUsuario) {
        return ResponseEntity.ok(tarefaService.listarTarefasUsuario(idUsuario));
    }
}
