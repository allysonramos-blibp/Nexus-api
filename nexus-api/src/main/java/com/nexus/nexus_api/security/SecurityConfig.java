package com.nexus.nexus_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsRaw;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        List<String> allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(
                                restAuthenticationEntryPoint
                        )
                        .accessDeniedHandler(
                                restAccessDeniedHandler
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/register"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}