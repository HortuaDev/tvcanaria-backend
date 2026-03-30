package com.tvcanaria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tvcanaria.dto.profile.ReporterProfileResponse;
import com.tvcanaria.service.ReporterService;

@RestController
@RequestMapping("/api/reporter")
public class ReporterController {

    @Autowired
    private ReporterService reporterService;

    @GetMapping("/{id}")
    public ResponseEntity<ReporterProfileResponse> getReporterChannel(@PathVariable Integer id) {
        ReporterProfileResponse reporter = reporterService.getReporter(id);
        if (reporter == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(reporter);
    }

}
