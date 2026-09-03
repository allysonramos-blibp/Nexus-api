package com.nexus.nexus_api.util;

import com.nexus.nexus_api.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Ponto único de checagem de "dono do recurso".
 *
 * Toda operação que recebe um id de usuário ou carrega uma entidade
 * dona de um usuário deve passar por aqui antes de prosseguir.
 *
 * Regra:
 * um usuário autenticado só pode ler/alterar/excluir recursos
 * que pertencem a ele mesmo.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Retorna o ID do usuário autenticado na requisição atual.
     */
    public static Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null ||
                        !(authentication.getPrincipal()
                                instanceof UserPrincipal principal)
        ) {
            throw new AccessDeniedException(
                    "Usuário não autenticado."
            );
        }

        return principal.getId();
    }

    /**
     * Garante que o dono real do recurso é o usuário autenticado.
     *
     * Lança 403 caso contrário.
     */
    public static void assertOwnership(
            Long resourceOwnerId
    ) {

        Long currentUserId =
                getCurrentUserId();

        if (
                resourceOwnerId == null ||
                        !resourceOwnerId.equals(currentUserId)
        ) {
            throw new AccessDeniedException(
                    "Você não tem permissão para acessar este recurso."
            );
        }
    }
}