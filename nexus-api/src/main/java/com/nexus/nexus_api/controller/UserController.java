package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.RegisterRequest;
import com.nexus.nexus_api.dto.UserResponse;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Recebe um DTO dedicado (nunca a entidade User) para impedir mass assignment (ex.: cliente
     * enviando um "id" arbitrário) e devolve UserResponse, que nunca contém a senha/hash.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User savedUser = userService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(savedUser));
    }
}
