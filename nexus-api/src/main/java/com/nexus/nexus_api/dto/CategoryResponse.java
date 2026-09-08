package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Category;
import com.nexus.nexus_api.model.CategoryType;

public record CategoryResponse(
        Long id,
        String nome,
        CategoryType tipo,
        String cor
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getNome(), category.getTipo(), category.getCor());
    }
}
