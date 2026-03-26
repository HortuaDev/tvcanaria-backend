package com.tvcanaria.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.dto.category.UserCategoryRequest;
import com.tvcanaria.dto.profile.UpdateProfileRequest;
import com.tvcanaria.dto.profile.UserProfileResponse;
import com.tvcanaria.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateUserProfile(authentication.getName(), request));
    }

    @GetMapping("/{id}/categories")
    public ResponseEntity<Set<CategoryResponse>> getUserCategories(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserCategories(id));
    }

    @PutMapping("/{id}/categories")
    public ResponseEntity<Set<CategoryResponse>> updateUserCategories(
            @PathVariable Integer id,
            @RequestBody UserCategoryRequest request) {
        return ResponseEntity.ok(userService.updateUserCategories(id, request.getCategoryIds()));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserProfileResponse> toggleUserStatus(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason) {

        // Llamamos al nuevo método del servicio
        UserProfileResponse updatedUser = userService.toggleUserStatus(id, reason);
        return ResponseEntity.ok(updatedUser);
    }
}