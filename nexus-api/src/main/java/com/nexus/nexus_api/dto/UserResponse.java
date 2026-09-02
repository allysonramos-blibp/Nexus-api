package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.User;
import lombok.Getter;

/**
 * DTO de saída para dados de usuário. Nunca inclui a senha (nem o hash),
 * mesmo que a entidade User seja alterada no futuro.
 */
@Getter
public class UserResponse {

    private final Long id;
    private final String email;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
    }

    public UserResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }
}
