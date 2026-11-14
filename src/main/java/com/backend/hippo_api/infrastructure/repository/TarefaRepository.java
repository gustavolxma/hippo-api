package com.backend.hippo_api.infrastructure.repository;

import com.backend.hippo_api.infrastructure.entity.Tarefa;
import com.backend.hippo_api.infrastructure.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findByUsuario(Usuario usuario);
    Optional<Tarefa> findByIdAndUsuario(Long id, Usuario usuario);
}
