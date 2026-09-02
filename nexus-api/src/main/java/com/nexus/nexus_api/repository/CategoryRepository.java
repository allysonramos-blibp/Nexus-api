package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Category;
import com.nexus.nexus_api.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdAndTipo(Long userId, CategoryType tipo);
}
