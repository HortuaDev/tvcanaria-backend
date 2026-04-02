package com.tvcanaria.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.entity.Category;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.CategoryRepository;

/**
 * Servicio para la gestión de categorías.
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Devuelve todas las categorías disponibles.
     *
     * @return lista de todas las categorías
     */
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene entidades {@link Category} a partir de un conjunto de IDs.
     * Lanza excepción si algún ID no existe.
     *
     * @param categoryIds conjunto de IDs de categorías
     * @return conjunto de entidades {@link Category} encontradas
     */
    public Set<Category> getCategoriesByIds(Set<Integer> categoryIds) {
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));

        if (categories.size() != categoryIds.size()) {
            Set<Integer> foundIds = categories.stream()
                    .map(Category::getCategoryId)
                    .collect(Collectors.toSet());

            Set<Integer> notFoundIds = categoryIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());

            throw new ResourceNotFoundException("Categorías no encontradas con IDs: " + notFoundIds);
        }
        return categories;
    }

    /**
     * Obtiene una categoría por su identificador.
     *
     * @param categoryId identificador de la categoría
     * @return la categoría encontrada
     */
    public CategoryResponse getCategoryById(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + categoryId));

        return mapToResponse(category);
    }

    /**
     * Actualiza el nombre de una categoría existente.
     *
     * @param id           identificador de la categoría a actualizar
     * @param categoryData objeto con los nuevos datos (solo {@code name} es actualizable)
     * @return la categoría actualizada
     */
    public CategoryResponse updateCategory(Integer id, Category categoryData) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        if (categoryData.getName() != null) {
            category.setName(categoryData.getName());
        }

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    /**
     * Elimina una categoría por su identificador.
     *
     * @param id identificador de la categoría a eliminar
     */
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se puede eliminar: Categoría no encontrada con ID " + id);
        }
        categoryRepository.deleteById(id);
    }

    /**
     * Convierte una entidad {@link Category} en su DTO de respuesta.
     *
     * @param category entidad a convertir
     * @return DTO {@link CategoryResponse}
     */
    private CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(category);
    }
}