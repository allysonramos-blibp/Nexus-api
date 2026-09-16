package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.MockExamRepository;
import com.nexus.nexus_api.repository.QuestionRepository;
import com.nexus.nexus_api.repository.StudyPlanRepository;
import com.nexus.nexus_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final MockExamRepository mockExamRepository;
    private final QuestionRepository questionRepository;

    public record AdminModulesDto(
            boolean estudos,
            boolean treinos,
            boolean financas,
            boolean iaExtracao
    ) {}

    public record AdminUserDto(
            Long id,
            String email,
            String role,
            boolean active,
            String status,
            String plan,
            AdminModulesDto modules,
            int pdfExtractCount,
            int pdfExtractLimit,
            long totalQuestoes,
            long simuladosCriados,
            long totalPlanos,
            String ultimoAcesso
    ) {}

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> listUsers() {
        List<User> users = userRepository.findAll();

        List<AdminUserDto> dtos = users.stream().map(u -> {
            boolean isAdmin = "allysonr510@gmail.com".equalsIgnoreCase(u.getEmail());

            List<StudyPlan> planos = studyPlanRepository.findByUserId(u.getId());
            long totalQuestoes = 0;
            for (StudyPlan p : planos) {
                totalQuestoes += questionRepository.countByTopicSubjectStudyPlanId(p.getId());
            }
            long totalSimulados = mockExamRepository.findByUserId(u.getId()).size();

            boolean moduloEstudos = isAdmin || u.isModuloEstudos();
            boolean moduloTreinos = isAdmin || u.isModuloTreinos();
            boolean moduloFinancas = isAdmin || u.isModuloFinancas();
            boolean moduloIaExtracao = isAdmin || u.isModuloIaExtracao();

            AdminModulesDto modules = new AdminModulesDto(
                    moduloEstudos,
                    moduloTreinos,
                    moduloFinancas,
                    moduloIaExtracao
            );

            boolean isActive = isAdmin || u.isActive();

            return new AdminUserDto(
                    u.getId(),
                    u.getEmail(),
                    isAdmin ? "ROLE_ADMIN" : (u.getRole() != null ? u.getRole() : "ROLE_USER"),
                    isActive,
                    isActive ? "ATIVO" : "SUSPENSO",
                    isAdmin ? "ENTERPRISE" : (u.getPlan() != null ? u.getPlan() : "PRO"),
                    modules,
                    u.getPdfExtractCount(),
                    isAdmin ? 99999 : u.getPdfExtractLimit(),
                    totalQuestoes,
                    totalSimulados,
                    planos.size(),
                    "Ativo no sistema"
            );
        }).toList();

        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));

        // Impedir suspensão do admin mestre
        if ("allysonr510@gmail.com".equalsIgnoreCase(user.getEmail())) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("userId", id);
            resp.put("status", "ATIVO");
            resp.put("message", "O administrador mestre não pode ser suspenso.");
            return ResponseEntity.ok(resp);
        }

        boolean active = true;
        if (body.containsKey("active")) {
            active = Boolean.parseBoolean(String.valueOf(body.get("active")));
        } else if (body.containsKey("status")) {
            active = "ATIVO".equalsIgnoreCase(String.valueOf(body.get("status")));
        }

        user.setActive(active);
        userRepository.save(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("userId", id);
        resp.put("active", active);
        resp.put("status", active ? "ATIVO" : "SUSPENSO");
        resp.put("message", "Status atualizado com sucesso!");
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/users/{id}/modules")
    public ResponseEntity<Map<String, Object>> updateModules(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));

        if (payload.containsKey("estudos")) {
            user.setModuloEstudos(Boolean.parseBoolean(String.valueOf(payload.get("estudos"))));
        }
        if (payload.containsKey("treinos")) {
            user.setModuloTreinos(Boolean.parseBoolean(String.valueOf(payload.get("treinos"))));
        }
        if (payload.containsKey("financas")) {
            user.setModuloFinancas(Boolean.parseBoolean(String.valueOf(payload.get("financas"))));
        }
        if (payload.containsKey("iaExtracao")) {
            user.setModuloIaExtracao(Boolean.parseBoolean(String.valueOf(payload.get("iaExtracao"))));
        }
        if (payload.containsKey("pdfExtractLimit")) {
            try {
                user.setPdfExtractLimit(Integer.parseInt(String.valueOf(payload.get("pdfExtractLimit"))));
            } catch (Exception ignored) {}
        }
        if (payload.containsKey("plan")) {
            user.setPlan(String.valueOf(payload.get("plan")));
        }

        userRepository.save(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("userId", id);
        resp.put("moduloEstudos", user.isModuloEstudos());
        resp.put("moduloTreinos", user.isModuloTreinos());
        resp.put("moduloFinancas", user.isModuloFinancas());
        resp.put("moduloIaExtracao", user.isModuloIaExtracao());
        resp.put("pdfExtractLimit", user.getPdfExtractLimit());
        resp.put("message", "Permissões e limites do usuário atualizados no banco.");
        return ResponseEntity.ok(resp);
    }
}
