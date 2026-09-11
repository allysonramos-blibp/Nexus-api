package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.TaskWorkflowStatus;
import jakarta.validation.constraints.NotNull;

public record TaskWorkflowStatusRequest(
        @NotNull(message = "O campo 'workflowStatus' é obrigatório.")
        TaskWorkflowStatus workflowStatus
) {}
