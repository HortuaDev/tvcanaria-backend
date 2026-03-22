package com.tvcanaria.dto.category;

import com.tvcanaria.entity.Category;

public class CategoryResponse {

    private Integer categoryId;
    private String name;

    public CategoryResponse(Category category) {
        this.categoryId = category.getCategoryId();
        this.name = category.getName();
    }

    public Integer getCategoryId() { return categoryId; }
    public String getName() { return name; }
}