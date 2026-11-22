package com.backend.hippo_api.business;

import com.backend.hippo_api.infrastructure.email.EmailService;
import com.backend.hippo_api.infrastructure.entity.CodigoVerificacao;
import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;
import com.backend.hippo_api.infrastructure.exceptions.CodigoInvalidoException;
import com.backend.hippo_api.infrastructure.exceptions.MuitasRequisicoesException;
import com.backend.hippo_api.infrastructure.repository.CodigoVerificacaoRepository;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CodigoVerificacaoService {

    // Regras dos Códigos (as mesmas que a tela do app mostra)
    static final Duration VALIDADE = Duration.ofMinutes(10);
    static final Duration ESPERA_PARA_REENVIAR = Duration.ofSeconds(45);
    static final int MAXIMO_TENTATIVAS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CodigoVerificacaoRepository codigoRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    // Método para gerar um novo Código e enviá-lo por Email (o anterior, se existir, deixa de valer)
    @Transactional
    public void gerarEEnviarCodigo(String email, String nome, FinalidadeCodigoEnum finalidade) {
        LocalDateTime agora = LocalDateTime.now(clock);

        // Impedir que o Usuário peça vários Códigos seguidos
        codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(email, finalidade)
                .filter(ultimo -> ultimo.getDataCriacao().plus(ESPERA_PARA_REENVIAR).isAfter(agora))
                .ifPresent(ultimo -> {
                    throw new MuitasRequisicoesException("Aguarde alguns segundos antes de pedir um novo Código!");
                });

        // Gerar o Código de 6 dígitos e guardar só o Hash dele
        String codigo = "%06d".formatted(RANDOM.nextInt(1_000_000));
        codigoRepository.deleteByEmailAndFinalidade(email, finalidade);
        codigoRepository.save(CodigoVerificacao.builder()
                .email(email)
                .finalidade(finalidade)
                .codigoHash(passwordEncoder.encode(codigo))
                .tentativas(0)
                .dataCriacao(agora)
                .dataExpiracao(agora.plus(VALIDADE))
                .build());

        // Enviar por Email (se falhar, a transação desfaz o Código salvo)
        emailService.enviarCodigoVerificacao(email, nome, codigo, finalidade);
    }

    // Método para conferir o Código digitado pelo Usuário. Se estiver certo, ele é apagado (só vale uma vez).
    // dontRollbackOn: mesmo quando o Código está errado, a tentativa precisa ficar salva.
    // Por isso quem chama este método não pode estar dentro de outra transação.
    @Transactional(dontRollbackOn = CodigoInvalidoException.class)
    public void validarCodigo(String email, FinalidadeCodigoEnum finalidade, String codigoInformado) {
        CodigoVerificacao codigo = codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(email, finalidade)
                .orElseThrow(() -> new CodigoInvalidoException("Código inválido ou expirado!"));

        // Código expirado ou com tentativas esgotadas não vale mais
        if (codigo.getDataExpiracao().isBefore(LocalDateTime.now(clock))
                || codigo.getTentativas() >= MAXIMO_TENTATIVAS) {
            codigoRepository.delete(codigo);
            throw new CodigoInvalidoException("Código inválido ou expirado! Peça um novo Código.");
        }

        if (codigoInformado == null || !passwordEncoder.matches(codigoInformado, codigo.getCodigoHash())) {
            codigo.setTentativas(codigo.getTentativas() + 1);
            codigoRepository.save(codigo);
            throw new CodigoInvalidoException("Código inválido ou expirado!");
        }

        codigoRepository.delete(codigo);
    }

    // Verificar se existe um Código pendente (usado para só permitir reenviar quando já houve um pedido)
    public boolean existeCodigoPendente(String email, FinalidadeCodigoEnum finalidade) {
        return codigoRepository.findTopByEmailAndFinalidadeOrderByDataCriacaoDesc(email, finalidade).isPresent();
    }
}
