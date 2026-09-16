package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.AuthResponse;
import com.nexus.nexus_api.dto.ForgotPasswordRequest;
import com.nexus.nexus_api.dto.LoginRequest;
import com.nexus.nexus_api.dto.ResetPasswordRequest;
import com.nexus.nexus_api.dto.UserResponse;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.security.JwtService;
import com.nexus.nexus_api.security.UserPrincipal;
import com.nexus.nexus_api.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        UserResponse userResponse = new UserResponse(principal);
        AuthResponse response = AuthResponse.of(token, jwtService.getExpirationMs(), userResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Map<String, Object> result = passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Senha redefinida com sucesso! Você já pode fazer login.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findById(principal.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(new UserResponse(principal));
        }

        return ResponseEntity.ok(new UserResponse(user));
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Módulo de autenticação Nexus operacional!");
        return ResponseEntity.ok(response);
    }
}
