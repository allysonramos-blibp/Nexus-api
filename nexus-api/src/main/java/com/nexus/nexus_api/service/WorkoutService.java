package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.WorkoutExerciseDto;
import com.nexus.nexus_api.dto.WorkoutRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.model.Workout;
import com.nexus.nexus_api.model.WorkoutExercise;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.repository.WorkoutRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public Workout create(WorkoutRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        Workout workout = Workout.builder()
                .grupoMuscular(request.grupoMuscular())
                .exerciciosExecutados(request.exerciciosExecutados())
                .dataTreino(request.dataTreino())
                .concluido(request.concluido())
                .user(user)
                .build();

        if (request.exercicios() != null) {
            List<WorkoutExercise> exercicios = new ArrayList<>();
            for (WorkoutExerciseDto dto : request.exercicios()) {
                exercicios.add(WorkoutExercise.builder()
                        .nome(dto.nome())
                        .series(dto.series())
                        .repeticoes(dto.repeticoes())
                        .carga(dto.carga())
                        .workout(workout)
                        .build());
            }
            workout.setExercicios(exercicios);
        }

        return workoutRepository.save(workout);
    }

    public List<Workout> listByUser(Long userId) {
        SecurityUtils.assertOwnership(userId);
        return workoutRepository.findByUserId(userId);
    }

    public Workout uploadImage(Long workoutId, MultipartFile file) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Treino não encontrado com ID: " + workoutId));

        SecurityUtils.assertOwnership(workout.getUser().getId());

        String storedFilename = fileStorageService.store(file);
        workout.setImagemUrl("/api/workouts/image/" + storedFilename);

        return workoutRepository.save(workout);
    }

    /**
     * Serve a imagem de um treino. Duas proteções aqui, que antes não existiam:
     *  1) Sanitização do nome de arquivo (bloqueia "../", "/", "\\") — corrige risco de path traversal.
     *  2) Ownership: a imagem só é servida se pertencer a um treino do usuário autenticado.
     */
    public InputStream getImageStream(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean belongsToCurrentUser = workoutRepository.existsByUserIdAndImagemUrlEndingWith(currentUserId, filename);

        if (!belongsToCurrentUser) {
            throw new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
        }

        return fileStorageService.load(filename);
    }
}
