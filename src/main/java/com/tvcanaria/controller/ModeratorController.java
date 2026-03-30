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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/moderators")
public class ModeratorController {

    @Autowired
    private ModeratorService moderatorService;

    @GetMapping("/{moderatorId}/reporters")
    public ResponseEntity<List<UserSummaryResponse>> getReportersByModerator(
            @PathVariable Integer moderatorId) {
        return ResponseEntity.ok(moderatorService.getReportersByModerator(moderatorId));
    }

    @GetMapping("/reporters/{reporterId}")
    public ResponseEntity<List<UserSummaryResponse>> getModeratorsByReporter(@PathVariable Integer reporterId) {
        return ResponseEntity.ok(moderatorService.getModeratorsByReporter(reporterId));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<?> sendRequest(@Valid @RequestBody ModeratorRequest request, Authentication authentication) {
        moderatorService.sendRequest(request, authentication);
        return ResponseEntity.ok("Solicitud enviada");
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<ModeratorResponse>> getPendingRequests(Authentication authentication) {
        return ResponseEntity.ok(moderatorService.getPendingRequests(authentication));
    }

    @PutMapping("/requests/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.acceptRequest(id, authentication);
        return ResponseEntity.ok("Solicitud aceptada");
    }

    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.rejectRequest(id, authentication);
        return ResponseEntity.ok("Solicitud rechazada");
    }

    @GetMapping("/is-moderator-of/{reporterId}")
    public ResponseEntity<Boolean> isModerator(@PathVariable Integer reporterId, Authentication authentication) {
        Integer userId = Integer.valueOf(authentication.getName());
        boolean result = moderatorService.isModeratorOf(userId, reporterId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/requests/my-requests")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<List<ModeratorResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(moderatorService.getMyRequests(authentication));
    }

    @GetMapping("/search-user")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<UserSummaryResponse> searchUser(
            @RequestParam String query,
            Authentication authentication) {
        return ResponseEntity.ok(moderatorService.searchUser(query, authentication));
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<?> cancelRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.cancelRequest(id, authentication);
        return ResponseEntity.noContent().build();
    }
}