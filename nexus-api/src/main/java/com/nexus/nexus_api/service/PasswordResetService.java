package com.nexus.nexus_api.service;

import com.nexus.nexus_api.model.PasswordResetToken;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.PasswordResetTokenRepository;
import com.nexus.nexus_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Gera um token de redefinição numérico de 6 dígitos válido por 15 minutos.
     * Retorna o token para notificação ao usuário.
     */
    @Transactional
    public Map<String, Object> requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            // Retorna sucesso genérico para proteção contra Enumeração de Usuários (OWASP)
            log.info("Solicitação de recuperação para e-mail não cadastrado: {}", normalizedEmail);
            return Map.of(
                    "success", true,
                    "message", "Se o e-mail estiver cadastrado, um código de verificação foi gerado."
            );
        }

        // Gera código de 6 dígitos seguro
        int code = 100000 + RANDOM.nextInt(900000);
        String tokenStr = String.valueOf(code);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(normalizedEmail)
                .token(tokenStr)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        log.info("🔐 [NEXUS RESET PASSWORD] Código gerado para {}: {} (válido por 15min)", normalizedEmail, tokenStr);

        return Map.of(
                "success", true,
                "message", "Código de verificação gerado com sucesso.",
                "email", normalizedEmail,
                "code", tokenStr // Retornado na resposta para permitir teste imediato e uso sem servidor SMTP externo
        );
    }

    /**
     * Valida o token e atualiza a senha do usuário com hash BCrypt seguro.
     */
    @Transactional
    public void resetPassword(String email, String token, String newPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedToken = token.trim();

        PasswordResetToken resetToken = tokenRepository
                .findFirstByEmailAndTokenAndUsedFalseOrderByExpiresAtDesc(normalizedEmail, normalizedToken)
                .orElseThrow(() -> new IllegalArgumentException("Código de recuperação inválido ou inexistente."));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Este código de recuperação expirou. Solicite um novo código.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para o e-mail informado."));

        // Atualiza a senha com hash BCrypt
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalida o token usado
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("✅ Senha redefinida com sucesso para o usuário {}", normalizedEmail);
    }
}
