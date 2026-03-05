package com.tvcanaria.controller;

import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.service.ModeratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderators")
public class ModeratorController {

    @Autowired
    private ModeratorService moderatorService;

    // Obtener reporteros asignados a un moderador
    @GetMapping("/{moderatorId}/reporters")
    public ResponseEntity<List<UserSummaryResponse>> getReportersByModerator(
            @PathVariable Integer moderatorId) {
        List<UserSummaryResponse> reporters = moderatorService.getReportersByModerator(moderatorId);
        return ResponseEntity.ok(reporters);
    }

    // Obtener moderadores de un reportero
    @GetMapping("/reporters/{reporterId}")
    public ResponseEntity<List<UserSummaryResponse>> getModeratorsByReporter(
            @PathVariable Integer reporterId) {
        List<UserSummaryResponse> moderators = moderatorService.getModeratorsByReporter(reporterId);
        return ResponseEntity.ok(moderators);
    }
}