package com.nexus.nexus_api.service;


import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.model.WorkoutGoal;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.repository.WorkoutGoalRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkoutGoalService {

    private final WorkoutGoalRepository workoutGoalRepository;
    private final UserRepository userRepository;

    public WorkoutGoal setGoal(Long userId, Integer metaTreinosPorSemana) {
        SecurityUtils.assertOwnership(userId);

        if (metaTreinosPorSemana == null || metaTreinosPorSemana < 0) {
            throw new IllegalArgumentException("O campo 'metaTreinosPorSemana' deve ser um número não negativo.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));

        WorkoutGoal goal = workoutGoalRepository.findByUserId(userId)
                .orElse(WorkoutGoal.builder().user(user).build());

        goal.setMetaTreinosPorSemana(metaTreinosPorSemana);
        return workoutGoalRepository.save(goal);
    }

    public WorkoutGoal getGoal(Long userId) {
        SecurityUtils.assertOwnership(userId);

        return workoutGoalRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));
                    // Objeto "vazio" apenas para resposta, nunca persistido sem meta real
                    // (o builder antigo criava WorkoutGoal sem user, o que quebraria o nullable=false
                    // do banco caso fosse salvo por engano em algum fluxo futuro).
                    return WorkoutGoal.builder().user(user).metaTreinosPorSemana(0).build();
                });
    }
}
