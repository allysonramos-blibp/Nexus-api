package com.nexus.nexus_api.dto;

import java.util.List;

public record PlanQuestionImportRequest(List<QuestionGroupImportRequest> grupos) {
}
