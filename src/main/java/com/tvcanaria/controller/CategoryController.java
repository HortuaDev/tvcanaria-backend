package com.tvcanaria.controller;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la consulta de categorías.
 * Base path: /api/categories
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Devuelve todas las categorías disponibles.
     *
     * @return {@code 200 OK} con la lista de categorías
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Obtiene una categoría por su identificador.
     *
     * @param id identificador de la categoría
     * @return {@code 200 OK} con la categoría encontrada
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Integer id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

}