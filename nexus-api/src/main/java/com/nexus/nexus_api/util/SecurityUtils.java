package com.nexus.nexus_api.util;

import com.nexus.nexus_api.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

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