package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.TaskRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Category;
import com.nexus.nexus_api.model.Task;
import com.nexus.nexus_api.model.TaskStatus;
import com.nexus.nexus_api.model.TaskWorkflowStatus;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.CategoryRepository;
import com.nexus.nexus_api.repository.TaskRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /** Mesma checagem de dono usada em Financeiro: categoria precisa ser do mesmo usuário. */
    private Category resolveCategory(Long categoryId, Long userId) {
        if (categoryId == null) return null;
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + categoryId));
        if (!category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Você não tem permissão para usar esta categoria.");
        }
        return category;
    }

    /**
     * Cria uma tarefa para o usuário autenticado. O dono NUNCA vem do corpo da requisição
     * — vem sempre do token JWT.
     *
     * Tarefas de edital (ehTopicoEdital=true) usam exclusivamente o TaskStatus legado
     * (status), como sempre funcionou. Tarefas comuns usam exclusivamente o
     * TaskWorkflowStatus novo (workflowStatus), começando em PENDENTE.
     */
    public Task create(TaskRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        boolean edital = Boolean.TRUE.equals(request.ehTopicoEdital());

        Task task = Task.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .status(edital && request.status() != null ? request.status() : TaskStatus.PENDENTE)
                .workflowStatus(edital ? null : (request.workflowStatus() != null ? request.workflowStatus() : TaskWorkflowStatus.PENDENTE))
                .prioridade(request.prioridade())
                .dataLimite(request.dataLimite())
                .horario(request.horario())
                .concluidaEm(!edital && request.workflowStatus() == TaskWorkflowStatus.CONCLUIDA ? LocalDateTime.now() : null)
                .ehTopicoEdital(request.ehTopicoEdital())
                .category(resolveCategory(request.categoryId(), currentUserId))
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

    private Task findOwned(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + id));
        SecurityUtils.assertOwnership(task.getUser().getId());
        return task;
    }

    /** Progresso de estudo do tópico de edital — endpoint e comportamento inalterados. */
    public Task updateStatus(Long id, TaskStatus status) {
        Task task = findOwned(id);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    /** Novo: status de fluxo de uma tarefa comum. Registra/limpa concluidaEm de acordo. */
    @Transactional
    public Task updateWorkflowStatus(Long id, TaskWorkflowStatus workflowStatus) {
        Task task = findOwned(id);

        task.setWorkflowStatus(workflowStatus);
        if (workflowStatus == TaskWorkflowStatus.CONCLUIDA) {
            task.setConcluidaEm(LocalDateTime.now());
        } else {
            task.setConcluidaEm(null);
        }

        return taskRepository.save(task);
    }

    /**
     * Atualiza os dados editáveis da tarefa (título, descrição, prioridade, data,
     * horário, categoria). Não mexe em status/workflowStatus/ehTopicoEdital/concluidaEm —
     * essas mudanças têm suas próprias ações dedicadas, então um PUT de edição de
     * conteúdo nunca apaga o estado de progresso da tarefa por engano.
     */
    @Transactional
    public Task update(Long id, TaskRequest request) {
        Task task = findOwned(id);

        task.setTitulo(request.titulo());
        task.setDescricao(request.descricao());
        task.setPrioridade(request.prioridade());
        task.setDataLimite(request.dataLimite());
        task.setHorario(request.horario());
        task.setCategory(resolveCategory(request.categoryId(), task.getUser().getId()));

        return taskRepository.save(task);
    }

    @Transactional
    public void delete(Long id) {
        Task task = findOwned(id);
        taskRepository.delete(task);
    }
}
