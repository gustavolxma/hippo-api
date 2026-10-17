package com.backend.hippo_api.controller;

import com.backend.hippo_api.business.TarefaService;
import com.backend.hippo_api.business.dtos.in.TarefaAlterarDadosDTORequest;
import com.backend.hippo_api.business.dtos.in.TarefaCadastroDTORequest;
import com.backend.hippo_api.business.dtos.out.*;

import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.enums.StatusTarefaEnum;
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
    public ResponseEntity<List<TarefaBuscaDadosDTOResponse>> listarTarefas(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(tarefaService.listarTarefas(usuario));
    }

    @GetMapping("/tarefa")
    public ResponseEntity<TarefaBuscaPorIdDTOResponse> buscarTarefaPorId(@RequestParam("id") Long id,
                                                                         @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(tarefaService.buscarTarefaPorId(id, usuario));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletarTarefaPorId(@RequestParam("id") Long id,
                                                   @AuthenticationPrincipal Usuario usuario) {
        tarefaService.deletarTarefa(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping
    public ResponseEntity<TarefaAlterarStatusDTOResponse> alterarStatusTarefaPorId(@RequestParam("id") Long id,
                                                                                   @RequestParam("status") StatusTarefaEnum status,
                                                                                   @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(tarefaService.alterarStatusTarefa(id, status, usuario));
    }

    @PutMapping
    public ResponseEntity<TarefaAlterarDadosDTOResponse> alterarDadosTarefaPorId(@RequestParam("id") Long id,
                                                                                      @RequestBody TarefaAlterarDadosDTORequest tarefaDTO,
                                                                                      @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(tarefaService.alterarDadosTarefa(id, tarefaDTO, usuario));
    }
}
