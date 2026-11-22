package com.backend.hippo_api.business;

import com.backend.hippo_api.business.converter.UsuarioConverter;
import com.backend.hippo_api.business.dtos.in.ReenviarCodigoDTORequest;
import com.backend.hippo_api.business.dtos.in.UsuarioAlterarDadosDTORequest;
import com.backend.hippo_api.business.dtos.in.UsuarioCadastroDTORequest;
import com.backend.hippo_api.business.dtos.in.VerificarCodigoDTORequest;
import com.backend.hippo_api.business.dtos.out.TokenDTOResponse;
import com.backend.hippo_api.business.dtos.out.UsuarioBuscaDadosDTOResponse;
import com.backend.hippo_api.business.dtos.out.UsuarioCadastroDTOResponse;
import com.backend.hippo_api.infrastructure.entity.Usuario;
import com.backend.hippo_api.infrastructure.enums.FinalidadeCodigoEnum;
import com.backend.hippo_api.infrastructure.exceptions.CodigoInvalidoException;
import com.backend.hippo_api.infrastructure.exceptions.ConflictException;
import com.backend.hippo_api.infrastructure.exceptions.RequisicaoInvalidaException;
import com.backend.hippo_api.infrastructure.exceptions.ResourceNotFoundException;
import com.backend.hippo_api.infrastructure.repository.UsuarioRepository;
import com.backend.hippo_api.infrastructure.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioConverter usuarioConverter;
    private final PasswordEncoder passwordEncoder;
    private final CodigoVerificacaoService codigoVerificacaoService;
    private final JwtUtil jwtUtil;
    private final Clock clock;

    // Método para cadastrar um Usuário. A conta só fica ativa depois de confirmar o Código enviado por Email.
    public UsuarioCadastroDTOResponse cadastrarUsuario(UsuarioCadastroDTORequest usuarioDTO) {
        Optional<Usuario> existente = usuarioRepository.findByEmail(usuarioDTO.getEmail());

        // Email já confirmado por outra conta: não pode cadastrar de novo
        if (existente.isPresent() && existente.get().isEmailVerificado()) {
            throw new ConflictException("Erro: Email " + usuarioDTO.getEmail() + " já existe!");
        }

        // Se o Cadastro anterior nunca foi confirmado, reaproveita a conta com os dados novos
        Usuario usuario = existente.orElseGet(() -> usuarioConverter.converterParaUsuarioCadastro(usuarioDTO));
        usuario.setNome(usuarioDTO.getNome());

        // Criptografar a Senha e setar a Data do Cadastro
        usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        usuario.setDataCadastro(LocalDateTime.now(clock));
        usuario.setEmailVerificado(false);

        // Salvar no Banco de Dados e enviar o Código de confirmação
        Usuario salvo = usuarioRepository.save(usuario);
        codigoVerificacaoService.gerarEEnviarCodigo(salvo.getEmail(), salvo.getNome(), FinalidadeCodigoEnum.CADASTRO);

        return usuarioConverter.converterParaUsuarioDTOCadastro(salvo);
    }

    // Método para confirmar o Cadastro com o Código recebido por Email. Devolve o Token de acesso.
    public TokenDTOResponse verificarCadastro(VerificarCodigoDTORequest verificarDTO) {
        return confirmarCodigoEGerarToken(verificarDTO, FinalidadeCodigoEnum.CADASTRO);
    }

    // Método chamado depois de conferir Email e Senha: envia o Código de acesso por Email
    public void enviarCodigoLogin(String email) {
        Usuario usuario = buscarPorEmail(email);
        codigoVerificacaoService.gerarEEnviarCodigo(usuario.getEmail(), usuario.getNome(), FinalidadeCodigoEnum.LOGIN);
    }

    // Método para concluir o Login com o Código recebido por Email. Devolve o Token de acesso.
    public TokenDTOResponse verificarLogin(VerificarCodigoDTORequest verificarDTO) {
        return confirmarCodigoEGerarToken(verificarDTO, FinalidadeCodigoEnum.LOGIN);
    }

    // Método para reenviar o Código do Cadastro ou do Login.
    // Só funciona se já existir um pedido pendente, para ninguém disparar Emails para qualquer endereço.
    public void reenviarCodigo(ReenviarCodigoDTORequest reenviarDTO) {
        FinalidadeCodigoEnum finalidade = reenviarDTO.getFinalidade();
        if (finalidade == FinalidadeCodigoEnum.ALTERAR_CONTA) {
            throw new CodigoInvalidoException("Para alterar Email ou Senha, peça o Código pelo endpoint autenticado!");
        }
        if (!codigoVerificacaoService.existeCodigoPendente(reenviarDTO.getEmail(), finalidade)) {
            throw new CodigoInvalidoException("Não há Código pendente para este Email!");
        }

        Usuario usuario = buscarPorEmail(reenviarDTO.getEmail());
        codigoVerificacaoService.gerarEEnviarCodigo(usuario.getEmail(), usuario.getNome(), finalidade);
    }

    // Método para enviar o Código que autoriza alterar Email ou Senha (vai para o Email atual)
    public void enviarCodigoAlterarConta(Usuario usuarioLogado) {
        Usuario usuario = buscarPorId(usuarioLogado.getId());
        codigoVerificacaoService.gerarEEnviarCodigo(usuario.getEmail(), usuario.getNome(), FinalidadeCodigoEnum.ALTERAR_CONTA);
    }

    // Método para alterar Email e/ou Senha. Só altera o que vier preenchido.
    // Devolve um Token novo: o Token guarda o Email, então o antigo para de valer se o Email mudar.
    public TokenDTOResponse alterarDadosUsuario(Usuario usuarioLogado, UsuarioAlterarDadosDTORequest alterarDTO) {
        Usuario usuario = buscarPorId(usuarioLogado.getId());

        String novoEmail = textoOuNulo(alterarDTO.getNovoEmail());
        boolean alterarEmail = novoEmail != null && !novoEmail.equalsIgnoreCase(usuario.getEmail());
        boolean alterarSenha = textoOuNulo(alterarDTO.getNovaSenha()) != null;

        // Conferir o pedido antes de gastar o Código
        if (!alterarEmail && !alterarSenha) {
            throw new RequisicaoInvalidaException("Informe um novo Email ou uma nova Senha!");
        }
        if (alterarEmail && usuarioRepository.existsByEmail(novoEmail)) {
            throw new ConflictException("Erro: Email " + novoEmail + " já existe!");
        }

        codigoVerificacaoService.validarCodigo(usuario.getEmail(), FinalidadeCodigoEnum.ALTERAR_CONTA, alterarDTO.getCodigo());

        if (alterarEmail) {
            usuario.setEmail(novoEmail);
        }
        if (alterarSenha) {
            usuario.setSenha(passwordEncoder.encode(alterarDTO.getNovaSenha()));
        }
        Usuario salvo = usuarioRepository.save(usuario);

        return TokenDTOResponse.builder()
                .token("Bearer " + jwtUtil.generateToken(salvo.getEmail()))
                .usuario(usuarioConverter.converterParaUsuarioDTOBuscarDados(salvo))
                .build();
    }

    public UsuarioBuscaDadosDTOResponse buscarUsuarioPorId(Long id) {
        return usuarioConverter.converterParaUsuarioDTOBuscarDados(
                usuarioRepository.findById(id).orElseThrow(
                        () -> new ResourceNotFoundException("Erro: Usuário de ID " + id + " não Encontrado!")
                )
        );
    }

    // Confere o Código, marca o Email como verificado e gera o Token (mesmo fluxo no Cadastro e no Login)
    private TokenDTOResponse confirmarCodigoEGerarToken(VerificarCodigoDTORequest verificarDTO,
                                                       FinalidadeCodigoEnum finalidade) {
        // Não revelar se o Email existe: Email desconhecido também é "Código inválido"
        Usuario usuario = usuarioRepository.findByEmail(verificarDTO.getEmail()).orElseThrow(
                () -> new CodigoInvalidoException("Código inválido ou expirado!")
        );

        codigoVerificacaoService.validarCodigo(usuario.getEmail(), finalidade, verificarDTO.getCodigo());

        if (!usuario.isEmailVerificado()) {
            usuario.setEmailVerificado(true);
            usuario = usuarioRepository.save(usuario);
        }

        return TokenDTOResponse.builder()
                .token("Bearer " + jwtUtil.generateToken(usuario.getEmail()))
                .usuario(usuarioConverter.converterParaUsuarioDTOBuscarDados(usuario))
                .build();
    }

    // O Usuário do Token pode estar desatualizado, então sempre buscamos a versão do Banco
    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Erro: Usuário de ID " + id + " não Encontrado!")
        );
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Erro: Usuário de Email " + email + " não Encontrado!")
        );
    }
}
