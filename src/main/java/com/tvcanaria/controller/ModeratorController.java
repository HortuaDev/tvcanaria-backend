package com.tvcanaria.controller;

import com.tvcanaria.dto.moderator.ModeratorRequest;
import com.tvcanaria.dto.moderator.ModeratorResponse;
import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.service.ModeratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderators")
public class ModeratorController {

    @Autowired
    private ModeratorService moderatorService;

    // ------------------- GET ----------------------

    @GetMapping("/{moderatorId}/reporters")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UserSummaryResponse>> getReportersByModerator(
            @PathVariable Integer moderatorId) {
        return ResponseEntity.ok(moderatorService.getReportersByModerator(moderatorId));
    }

    @GetMapping("/reporters/{reporterId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'REPORTER')")
    public ResponseEntity<List<UserSummaryResponse>> getModeratorsByReporter(@PathVariable Integer reporterId) {
        return ResponseEntity.ok(moderatorService.getModeratorsByReporter(reporterId));
    }

    @GetMapping("/requests/pending")
    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('READER')")
    public ResponseEntity<List<ModeratorResponse>> getPendingRequests(Authentication authentication) {
        return ResponseEntity.ok(moderatorService.getPendingRequests(authentication));
    }

    @GetMapping("/requests/my-requests")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<List<ModeratorResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(moderatorService.getMyRequests(authentication));
    }

    @GetMapping("/is-moderator-of/{reporterId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isModerator(@PathVariable Integer reporterId, Authentication authentication) {
        boolean result = moderatorService.isModeratorOf(authentication, reporterId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search-user")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<UserSummaryResponse> searchUser(
            @RequestParam String query,
            Authentication authentication) {
        return ResponseEntity.ok(moderatorService.searchUser(query, authentication));
    }

    // ------------------- POST ----------------------

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<?> sendRequest(@Valid @RequestBody ModeratorRequest request, Authentication authentication) {
        moderatorService.sendRequest(request, authentication);
        return ResponseEntity.ok("Solicitud enviada");
    }

    // ------------------- PUT ----------------------

    @PutMapping("/requests/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acceptRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.acceptRequest(id, authentication);
        return ResponseEntity.ok("Solicitud aceptada");
    }

    @PutMapping("/requests/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rejectRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.rejectRequest(id, authentication);
        return ResponseEntity.ok("Solicitud rechazada");
    }

    // ------------------- DELETE ----------------------

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<?> cancelRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.cancelRequest(id, authentication);
        return ResponseEntity.noContent().build();
    }
}