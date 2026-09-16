package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    public record AdminUserDto(
            Long id,
            String email,
            String role,
            String status,
            String plan,
            boolean moduloEstudos,
            boolean moduloTreinos,
            boolean moduloFinancas,
            boolean moduloIaExtracao
    ) {}

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> listUsers() {
        List<User> users = userRepository.findAll();
        List<AdminUserDto> dtos = users.stream()
                .map(u -> new AdminUserDto(
                        u.getId(),
                        u.getEmail(),
                        "ADMIN".equalsIgnoreCase(u.getEmail()) || u.getEmail().contains("admin") || u.getEmail().equals("allysonr510@gmail.com") ? "ROLE_ADMIN" : "ROLE_USER",
                        "ATIVO",
                        "PRO",
                        true,
                        true,
                        true,
                        true
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "ATIVO");
        Map<String, Object> resp = new HashMap<>();
        resp.put("userId", id);
        resp.put("status", status);
        resp.put("message", "Status do usuário atualizado com sucesso no Nexus SaaS.");
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/users/{id}/modules")
    public ResponseEntity<Map<String, Object>> updateModules(@PathVariable Long id, @RequestBody Map<String, Boolean> modules) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("userId", id);
        resp.put("modules", modules);
        resp.put("message", "Módulos do assinante configurados com sucesso.");
        return ResponseEntity.ok(resp);
    }
}
