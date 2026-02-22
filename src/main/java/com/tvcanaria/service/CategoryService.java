package com.tvcanaria.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tvcanaria.entity.Category;
import com.tvcanaria.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Obtener todas las categorías
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Obtener categorías por un set de IDs
    public Set<Category> getCategoriesByIds(Set<Integer> categoryId) {
        return new HashSet<>(categoryRepository.findAllById(categoryId));
    }

    // Obtener una categoría por ID
    public Category getCategoryById(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id " + categoryId));
    }

    // Crear una nueva categoría
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    // Actualizar categoría existente
    public Category updateCategory(Integer id, Category categoryData) {
        Category category = getCategoryById(id);
        if (categoryData.getName() != null) {
            category.setName(categoryData.getName());
        }
        return categoryRepository.save(category);
    }

    // Borrar categoría
    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }
}