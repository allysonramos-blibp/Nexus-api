package com.nexus.nexus_api.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CorsFilter implements Filter {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsRaw;

    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse res,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request =
                (HttpServletRequest) req;

        HttpServletResponse response =
                (HttpServletResponse) res;

        String origin =
                request.getHeader("Origin");

        List<String> allowedOrigins =
                Arrays.stream(
                                allowedOriginsRaw.split(",")
                        )
                        .map(String::trim)
                        .filter(
                                value -> !value.isBlank()
                        )
                        .toList();

        /*
         * Libera somente as origens configuradas.
         */
        if (
                origin != null &&
                        allowedOrigins.contains(origin)
        ) {
            response.setHeader(
                    "Access-Control-Allow-Origin",
                    origin
            );

            response.setHeader(
                    "Vary",
                    "Origin"
            );
        }

        response.setHeader(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, DELETE, OPTIONS"
        );

        response.setHeader(
                "Access-Control-Allow-Headers",
                "Authorization, Content-Type, Accept, X-Requested-With"
        );

        response.setHeader(
                "Access-Control-Allow-Credentials",
                "true"
        );

        response.setHeader(
                "Access-Control-Max-Age",
                "3600"
        );

        response.setHeader(
                "Access-Control-Expose-Headers",
                "Authorization"
        );

        /*
         * Preflight.
         */
        if (
                "OPTIONS".equalsIgnoreCase(
                        request.getMethod()
                )
        ) {
            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            return;
        }

        chain.doFilter(
                req,
                res
        );
    }
}