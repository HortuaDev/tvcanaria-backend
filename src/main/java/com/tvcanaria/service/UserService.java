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
import com.tvcanaria.entity.UserBlock;
import com.tvcanaria.repository.UserBlockRepository;
import com.tvcanaria.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final UserBlockRepository userBlockRepository;

    public UserService(UserRepository userRepository, CategoryService categoryService,
            UserBlockRepository userBlockRepository) {
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.userBlockRepository = userBlockRepository;
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

    @Transactional
    public UserProfileResponse toggleUserStatus(Integer userId, String reason) {
        User user = findUserById(userId);

        if (user.getIsActive()) {
            // LÓGICA DE DESACTIVACIÓN
            user.setIsActive(false);

            UserBlock block = new UserBlock();
            block.setUser(user);
            block.setReason(reason != null ? reason : "Desactivación indefinida por administrador");

            userBlockRepository.save(block);
            user.setUserBlock(block);

        } else {
            // LÓGICA DE ACTIVACIÓN
            user.setIsActive(true);

            UserBlock existingBlock = user.getUserBlock();
            if (existingBlock != null) {
                user.setUserBlock(null);
                userBlockRepository.delete(existingBlock);
            }
        }

        user = userRepository.save(user);
        return new UserProfileResponse(user);
    }

    private User findUserById(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}