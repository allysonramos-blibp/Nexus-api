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

    /**
     * Pode ser configurado no application.properties:
     *
     * cors.allowed-origins=https://seu-front.vercel.app,http://localhost:5173
     *
     * Para diagnóstico inicial, "*" permite qualquer origem.
     */
    @Value("${cors.allowed-origins:*}")
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

        String origin = request.getHeader("Origin");

        List<String> allowedOrigins =
                Arrays.stream(allowedOriginsRaw.split(","))
                        .map(String::trim)
                        .filter(originValue -> !originValue.isBlank())
                        .toList();

        /*
         * Verifica se a origem da requisição é permitida.
         */
        boolean allowAllOrigins =
                allowedOrigins.contains("*");

        boolean originIsAllowed =
                origin != null &&
                        (
                                allowAllOrigins ||
                                        allowedOrigins.contains(origin)
                        );

        /*
         * Access-Control-Allow-Origin
         */
        if (originIsAllowed) {
            response.setHeader(
                    "Access-Control-Allow-Origin",
                    origin
            );

            /*
             * Como a resposta varia de acordo com a origem,
             * informa aos caches/proxies que devem considerar
             * o header Origin.
             */
            response.setHeader(
                    "Vary",
                    "Origin"
            );
        }

        /*
         * Métodos permitidos.
         */
        response.setHeader(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, DELETE, OPTIONS"
        );

        /*
         * Headers permitidos.
         */
        response.setHeader(
                "Access-Control-Allow-Headers",
                "Authorization, Content-Type, Accept, X-Requested-With"
        );

        /*
         * Permite credenciais.
         *
         * Como estamos devolvendo a origem específica no
         * Access-Control-Allow-Origin, isso é compatível.
         */
        response.setHeader(
                "Access-Control-Allow-Credentials",
                "true"
        );

        /*
         * Permite que o navegador mantenha o resultado
         * do preflight em cache por 1 hora.
         */
        response.setHeader(
                "Access-Control-Max-Age",
                "3600"
        );

        /*
         * Expõe o Authorization para o frontend.
         */
        response.setHeader(
                "Access-Control-Expose-Headers",
                "Authorization"
        );

        /*
         * Preflight CORS.
         *
         * Não deixa o OPTIONS chegar ao JWT.
         */
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            return;
        }

        /*
         * Continua normalmente para Spring Security,
         * controllers e demais filtros.
         */
        chain.doFilter(req, res);
    }
}