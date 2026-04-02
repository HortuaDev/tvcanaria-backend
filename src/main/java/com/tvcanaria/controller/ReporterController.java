package com.tvcanaria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tvcanaria.dto.profile.ReporterProfileResponse;
import com.tvcanaria.service.ReporterService;

/**
 * Controlador REST para la consulta del perfil público de un reporter.
 * Base path: /api/reporter
 */
@RestController
@RequestMapping("/api/reporter")
public class ReporterController {

    @Autowired
    private ReporterService reporterService;

    /**
     * Devuelve el perfil público (canal) de un reporter.
     *
     * @param id identificador del reporter
     * @return {@code 200 OK} con el perfil del reporter,
     *         o {@code 404 Not Found} si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReporterProfileResponse> getReporterChannel(@PathVariable Integer id) {
        ReporterProfileResponse reporter = reporterService.getReporter(id);
        if (reporter == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reporter);
    }

}