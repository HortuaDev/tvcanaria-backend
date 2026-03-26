package com.tvcanaria.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.dto.profile.UpdateProfileRequest;
import com.tvcanaria.dto.profile.UserProfileResponse;
import com.tvcanaria.entity.Category;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public UserService(UserRepository userRepository, CategoryService categoryService) {
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    public List<UserProfileResponse> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserProfileResponse::new)
                .collect(Collectors.toList());
    }

    public UserProfileResponse getUserProfile(String userIdStr) {
        User user = findUserById(Integer.parseInt(userIdStr));
        return new UserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(String userIdStr, UpdateProfileRequest request) {
        User user = findUserById(Integer.parseInt(userIdStr));

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email ya en uso");
            }
            user.setEmail(request.getEmail());
        }

        return new UserProfileResponse(userRepository.save(user));
    }

    public Set<CategoryResponse> getUserCategories(Integer userId) {
        return findUserById(userId).getCategories().stream()
                .map(CategoryResponse::new).collect(Collectors.toSet());
    }

    @Transactional
    public Set<CategoryResponse> updateUserCategories(Integer userId, Set<Integer> categoryIds) {
        if (categoryIds.size() > 5)
            throw new RuntimeException("Máximo 5 categorías");

        User user = findUserById(userId);
        Set<Category> categories = categoryService.getCategoriesByIds(categoryIds);
        user.setCategories(categories);

        return categories.stream().map(CategoryResponse::new).collect(Collectors.toSet());
    }

    private User findUserById(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}