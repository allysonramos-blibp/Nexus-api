package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.TaskRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Task;
import com.nexus.nexus_api.model.TaskStatus;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.TaskRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    /**
     * Cria uma tarefa para o usuário autenticado. O dono NUNCA vem do corpo da requisição
     * (antes: task.getUser().getId() confiava cegamente no client) — vem sempre do token JWT.
     */
    public Task create(TaskRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        Task task = Task.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .status(request.status())
                .prioridade(request.prioridade())
                .dataLimite(request.dataLimite())
                .ehTopicoEdital(request.ehTopicoEdital())
                .user(user)
                .build();

        return taskRepository.save(task);
    }

    public List<Task> listByUser(Long userId) {
        SecurityUtils.assertOwnership(userId);
        return taskRepository.findByUserId(userId);
    }

    public List<Task> listEditalProgress(Long userId) {
        SecurityUtils.assertOwnership(userId);
        return taskRepository.findByUserIdAndEhTopicoEditalTrue(userId);
    }

    /** Só permite alterar o status se a tarefa pertencer ao usuário autenticado. */
    public Task updateStatus(Long id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + id));

        SecurityUtils.assertOwnership(task.getUser().getId());

        task.setStatus(status);
        return taskRepository.save(task);
    }
}
