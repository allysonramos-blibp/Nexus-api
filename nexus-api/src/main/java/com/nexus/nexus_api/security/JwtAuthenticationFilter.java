package com.nexus.nexus_api.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String HEADER_NAME =
            "Authorization";

    private static final String BEARER_PREFIX =
            "Bearer ";

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        /*
         * Preflight CORS não deve passar
         * pela validação JWT.
         */
        if (
                "OPTIONS".equalsIgnoreCase(
                        request.getMethod()
                )
        ) {
            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String authHeader =
                request.getHeader(
                        HEADER_NAME
                );

        /*
         * Sem Authorization:
         *
         * deixa o Spring Security decidir
         * se o endpoint é público ou protegido.
         */
        if (
                authHeader == null ||
                        !authHeader.startsWith(
                                BEARER_PREFIX
                        )
        ) {
            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authHeader
                        .substring(
                                BEARER_PREFIX.length()
                        )
                        .trim();

        /*
         * Bearer vazio.
         */
        if (token.isBlank()) {
            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        try {
            String email =
                    jwtService.extractEmail(
                            token
                    );

            if (
                    email != null &&
                            SecurityContextHolder
                                    .getContext()
                                    .getAuthentication() == null
            ) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(
                                        email
                                );

                if (
                        jwtService.isTokenValid(
                                token,
                                userDetails
                        )
                ) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authToken
                            );
                }
            }

        } catch (
                JwtException |
                IllegalArgumentException e
        ) {

            /*
             * Token inválido não deve derrubar
             * o servidor.
             */
            SecurityContextHolder
                    .clearContext();
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}