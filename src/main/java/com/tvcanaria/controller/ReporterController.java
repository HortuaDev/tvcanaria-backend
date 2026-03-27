package com.tvcanaria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.service.ModeratorService;

@RestController
@RequestMapping("/api/reporter")
public class ReporterController {

    @Autowired
    private ReporterService reporterService;
}
