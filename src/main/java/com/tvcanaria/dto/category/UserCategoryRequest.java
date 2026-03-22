package com.tvcanaria.dto.category;

import java.util.Set;

public class UserCategoryRequest {

    private Set<Integer> categoryIds;

    public Set<Integer> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(Set<Integer> categoryIds) {
        this.categoryIds = categoryIds;
    }
}