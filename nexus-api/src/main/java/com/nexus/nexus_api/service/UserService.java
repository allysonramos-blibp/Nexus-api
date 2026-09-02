package com.nexus.nexus_api.service;

import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cadastra um novo usuário. A senha chega em texto puro (vinda do DTO de registro) e é
     * transformada em hash BCrypt antes de qualquer persistência — o texto original nunca
     * toca o banco de dados.
     */
    public User register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build();

        return userRepository.save(user);
    }

    // A autenticação (verificação de e-mail/senha) passou a ser responsabilidade do
    // AuthenticationManager + CustomUserDetailsService + PasswordEncoder (ver pacote "security").
    // O método antigo "authenticate(email, password)" comparava senha em texto puro e foi removido.
}
