package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.CategoryRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Category;
import com.nexus.nexus_api.model.CategoryType;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.CategoryRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Category create(CategoryRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        Category category = Category.builder()
                .nome(request.nome())
                .tipo(request.tipo())
                .cor(request.cor())
                .user(user)
                .build();

        return categoryRepository.save(category);
    }

    public List<Category> listByUserAndTipo(Long userId, CategoryType tipo) {
        SecurityUtils.assertOwnership(userId);
        return categoryRepository.findByUserIdAndTipo(userId, tipo);
    }

    private Category findOwned(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));
        SecurityUtils.assertOwnership(category.getUser().getId());
        return category;
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = findOwned(id);
        category.setNome(request.nome());
        category.setTipo(request.tipo());
        category.setCor(request.cor());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findOwned(id);
        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // FK de financial_transactions.category_id — existe lançamento usando essa
            // categoria. Vira 409 com mensagem clara em vez de vazar erro de SQL.
            throw new IllegalStateException(
                    "Essa categoria está em uso por um ou mais lançamentos e não pode ser excluída.");
        }
    }
}
