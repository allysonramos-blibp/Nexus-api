package com.nexus.nexus_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    /**
     * Filtro CORS customizado.
     *
     * Ele é executado antes dos filtros do Spring Security,
     * permitindo que o navegador faça o preflight OPTIONS.
     */
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter();
    }

    /**
     * Codificador de senha.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provider responsável por localizar o usuário
     * e validar a senha utilizando BCrypt.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * AuthenticationManager utilizado no login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }

    /**
     * Configuração principal do Spring Security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // API REST não utiliza CSRF
                .csrf(csrf -> csrf.disable())

                // CORS será tratado pelo nosso CorsFilter
                .cors(cors -> cors.disable())

                // API stateless: autenticação através do JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Tratamento de 401 e 403
                .exceptionHandling(handling ->
                        handling
                                .authenticationEntryPoint(
                                        restAuthenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        restAccessDeniedHandler
                                )
                )

                // Regras de autorização
                .authorizeHttpRequests(auth -> auth

                        /*
                         * Preflight CORS
                         *
                         * O navegador envia OPTIONS antes de determinadas
                         * requisições POST/PUT/PATCH/DELETE.
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        /*
                         * Health check da API
                         *
                         * GET /api/auth
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth"
                        ).permitAll()

                        /*
                         * Login
                         *
                         * POST /api/auth/login
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        /*
                         * Cadastro
                         *
                         * POST /api/users/register
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/register"
                        ).permitAll()

                        /*
                         * Qualquer outro endpoint exige JWT.
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * JWT precisa executar antes do filtro padrão
                 * de autenticação por usuário/senha.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /*
                 * Nosso CORS precisa ficar antes do JWT.
                 *
                 * Isso é especialmente importante para OPTIONS.
                 */
                .addFilterBefore(
                        corsFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}