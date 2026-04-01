package com.tvcanaria.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.dto.profile.UpdateProfileRequest;
import com.tvcanaria.dto.profile.UserProfileResponse;
import com.tvcanaria.dto.user.CreateUserAdminRequest;
import com.tvcanaria.dto.user.UpdateUserAdminRequest;
import com.tvcanaria.entity.Category;
import com.tvcanaria.entity.User;
import com.tvcanaria.entity.UserBlock;
import com.tvcanaria.exception.BadRequestException;
import com.tvcanaria.exception.DuplicateResourceException;
import com.tvcanaria.exception.ForbiddenAccessException;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.UserBlockRepository;
import com.tvcanaria.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // ------------------- PERFIL Y USUARIO AUTENTICADO ----------------------

    public UserProfileResponse getUserProfile(Authentication auth) {
        User user = findUserById(getAuthenticatedUserId(auth));
        return new UserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(Authentication auth, UpdateProfileRequest request) {
        User user = findUserById(getAuthenticatedUserId(auth));

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("El email ya está en uso");
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
    public Set<CategoryResponse> updateUserCategories(Integer targetUserId, Set<Integer> categoryIds,
            Authentication auth) {
        Integer authenticatedUserId = getAuthenticatedUserId(auth);

        if (!targetUserId.equals(authenticatedUserId)) {
            throw new ForbiddenAccessException("No tienes permisos para modificar las categorías de otro usuario");
        }

        if (categoryIds.size() > 5) {
            throw new BadRequestException("No se pueden asignar más de 5 categorías");
        }

        User user = findUserById(targetUserId);
        Set<Category> categories = categoryService.getCategoriesByIds(categoryIds);
        user.setCategories(categories);
        userRepository.save(user);

        return categories.stream().map(CategoryResponse::new).collect(Collectors.toSet());
    }

    // ------------------- BÚSQUEDAS Y LISTADOS ----------------------

    public List<UserProfileResponse> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserProfileResponse::new)
                .collect(Collectors.toList());
    }

    public Page<UserProfileResponse> searchUsers(String query, String sortBy, String order, String dateFromStr,
            String dateToStr, int page, int size) {

        String sortProperty = "alphabetical".equals(sortBy) ? "username" : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortProperty);

        Pageable pageable = PageRequest.of(page, size, sort);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime dateFrom = null;
        LocalDateTime dateTo = null;

        try {
            if (dateFromStr != null && !dateFromStr.trim().isEmpty())
                dateFrom = LocalDate.parse(dateFromStr, formatter).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato de fecha inválido en 'dateFrom'. Use el formato yyyy-MM-dd");
        }

        try {
            if (dateToStr != null && !dateToStr.trim().isEmpty())
                dateTo = LocalDate.parse(dateToStr, formatter).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato de fecha inválido en 'dateTo'. Use el formato yyyy-MM-dd");
        }

        return userRepository.searchAndFilterUsers(query, dateFrom, dateTo, pageable)
                .map(UserProfileResponse::new);
    }

    // ------------------- ACCIONES DE ADMINISTRADOR ----------------------

    @Transactional
    public UserProfileResponse toggleUserStatus(Integer userId, String reason) {
        User user = findUserById(userId);

        if (user.getIsActive()) {
            user.setIsActive(false);

            UserBlock block = new UserBlock();
            block.setUser(user);
            block.setReason(reason != null ? reason : "Desactivación indefinida por administrador");
            block.setBlockedUntil(null);

            userBlockRepository.save(block);
            user.setUserBlock(block);

        } else {
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

    @Transactional
    public UserProfileResponse createUser(CreateUserAdminRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("El nombre de usuario ya existe");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("El email ya existe");

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);

        return new UserProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updateUserByAdmin(Integer userId, UpdateUserAdminRequest request) {
        User user = findUserById(userId);

        if (!user.getUsername().equals(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("El nombre de usuario ya está en uso");
        }
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("El email ya está en uso");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        return new UserProfileResponse(userRepository.save(user));
    }

    // ------------------- HELPERS ----------------------

    private Integer getAuthenticatedUserId(Authentication auth) {
        return Integer.valueOf(auth.getName());
    }

    private User findUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }
}