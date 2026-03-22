package com.tvcanaria.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.entity.Category;
import com.tvcanaria.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Obtener todas las categorías
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Obtener categorías por un set de IDs
    public Set<Category> getCategoriesByIds(Set<Integer> categoryIds) {
        return new HashSet<>(categoryRepository.findAllById(categoryIds));
    }

    // Obtener una categoría por ID
    public CategoryResponse getCategoryById(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id " + categoryId));

        return mapToResponse(category);
    }

    // Crear una nueva categoría
    public CategoryResponse createCategory(Category category) {
        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    // Actualizar categoría existente
    public CategoryResponse updateCategory(Integer id, Category categoryData) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id " + id));

        if (categoryData.getName() != null) {
            category.setName(categoryData.getName());
        }

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    // Borrar categoría
    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }

    private CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(category);
    }
}