package com.tvcanaria.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.dto.category.UserCategoryRequest;
import com.tvcanaria.dto.profile.UpdateProfileRequest;
import com.tvcanaria.dto.profile.UserProfileResponse;
import com.tvcanaria.dto.user.CreateUserAdminRequest;
import com.tvcanaria.dto.user.UpdateUserAdminRequest;
import com.tvcanaria.service.UserService;

import jakarta.validation.Valid;

/**
 * Controlador REST para la gestión de usuarios.
 * Base path: /api/users
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ------------------- LECTURA PÚBLICA / BÚSQUEDA ----------------------

    /**
     * Devuelve todos los usuarios paginados con filtros opcionales. Solo accesible
     * por ADMIN.
     *
     * @param search   término de búsqueda por nombre o email (opcional)
     * @param sortBy   campo de ordenación (por defecto "createdAt")
     * @param order    dirección de ordenación: "asc" o "desc"
     * @param dateFrom fecha de registro desde (yyyy-MM-dd, opcional)
     * @param dateTo   fecha de registro hasta (yyyy-MM-dd, opcional)
     * @param page     número de página (por defecto 0)
     * @param size     tamaño de página (por defecto 10)
     * @return {@code 200 OK} con página de usuarios
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "order", required = false, defaultValue = "desc") String order,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.searchUsers(search, sortBy, order, dateFrom, dateTo, page, size));
    }

    /**
     * Devuelve las categorías favoritas de un usuario.
     *
     * @param id identificador del usuario
     * @return {@code 200 OK} con el conjunto de categorías
     */
    @GetMapping("/{id}/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Set<CategoryResponse>> getUserCategories(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserCategories(id));
    }

    // ------------------- PERFIL DEL USUARIO AUTENTICADO ----------------------

    /**
     * Devuelve el perfil del usuario autenticado.
     *
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con el perfil del usuario
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserProfile(authentication));
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * @param request        nuevos datos del perfil
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con el perfil actualizado
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateUserProfile(authentication, request));
    }

    /**
     * Actualiza las categorías favoritas de un usuario.
     *
     * @param id             identificador del usuario
     * @param request        conjunto de IDs de categorías seleccionadas
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con el conjunto de categorías actualizado
     */
    @PutMapping("/{id}/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Set<CategoryResponse>> updateUserCategories(
            @PathVariable Integer id,
            @RequestBody UserCategoryRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateUserCategories(id, request.getCategoryIds(), authentication));
    }

    // ------------------- ADMINISTRACIÓN ----------------------

    /**
     * Crea un nuevo usuario desde el panel de administración.
     *
     * @param request datos del usuario a crear
     * @return {@code 200 OK} con el perfil del usuario creado
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserProfileResponse> createUser(@Valid @RequestBody CreateUserAdminRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    /**
     * Actualiza los datos de un usuario desde el panel de administración.
     *
     * @param id      identificador del usuario
     * @param request nuevos datos del usuario (rol, nombre, etc.)
     * @return {@code 200 OK} con el perfil actualizado
     */
    @PutMapping("/{id}/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateUserByAdmin(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserAdminRequest request) {
        return ResponseEntity.ok(userService.updateUserByAdmin(id, request));
    }

    /**
     * Activa o desactiva la cuenta de un usuario.
     *
     * @param id     identificador del usuario
     * @param reason motivo del cambio de estado (opcional)
     * @return {@code 200 OK} con el perfil actualizado
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserProfileResponse> toggleUserStatus(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason) {
        UserProfileResponse updatedUser = userService.toggleUserStatus(id, reason);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Activa o desactiva la cuenta de un usuario (el usuario mismo desde su
     * perfil).
     *
     * @param id identificador del usuario.
     * @param auth usuario autenticado.
     * @return {@code 200 OK} con el perfil actualizado.
     */
    @PutMapping("/{id}/toggle-status-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> toggleUserStatusProfile(
            @PathVariable Integer id, Authentication auth) {
        UserProfileResponse updatedUser = userService.toggleUserStatusProfile(id, auth);
        return ResponseEntity.ok(updatedUser);
    }
}