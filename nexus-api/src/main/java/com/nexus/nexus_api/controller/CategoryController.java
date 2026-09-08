package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.CategoryRequest;
import com.nexus.nexus_api.dto.CategoryResponse;
import com.nexus.nexus_api.model.Category;
import com.nexus.nexus_api.model.CategoryType;
import com.nexus.nexus_api.service.CategoryService;
import com.nexus.nexus_api.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        Category created = categoryService.create(request, SecurityUtils.getCurrentUserId());
        return CategoryResponse.from(created);
    }

    /** Sem tipo, lista as categorias FINANCEIRO do usuário (uso mais comum hoje). */
    @GetMapping
    public List<CategoryResponse> list(@RequestParam(required = false) CategoryType tipo) {
        CategoryType effectiveTipo = tipo != null ? tipo : CategoryType.FINANCEIRO;
        return categoryService.listByUserAndTipo(SecurityUtils.getCurrentUserId(), effectiveTipo).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category updated = categoryService.update(id, request);
        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
