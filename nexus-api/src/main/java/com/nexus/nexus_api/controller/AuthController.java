package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.AuthResponse;
import com.nexus.nexus_api.dto.LoginRequest;
import com.nexus.nexus_api.dto.UserResponse;
import com.nexus.nexus_api.security.JwtService;
import com.nexus.nexus_api.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Autentica com e-mail/senha via AuthenticationManager (que usa CustomUserDetailsService +
     * BCryptPasswordEncoder por baixo) e emite um JWT. Credenciais inválidas lançam
     * BadCredentialsException, tratada de forma genérica pelo GlobalExceptionHandler (401),
     * sem revelar se o e-mail existe ou não.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        UserResponse userResponse = new UserResponse(principal.getId(), principal.getUsername());
        AuthResponse response = AuthResponse.of(token, jwtService.getExpirationMs(), userResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Módulo de autenticação Nexus operacional!");
        return ResponseEntity.ok(response);
    }

}
