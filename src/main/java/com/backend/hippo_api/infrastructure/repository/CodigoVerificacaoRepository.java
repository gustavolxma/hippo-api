package com.backend.hippo_api.infrastructure.repository;

import com.backend.hippo_api.infrastructure.entity.CodigoVerificacao;
import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, Long> {
    Optional<CodigoVerificacao> findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(String email,
                                                                                 FinalidadeCodigoEnum finalidade);
    void deleteByEmailAndFinalidade(String email, FinalidadeCodigoEnum finalidade);
}
