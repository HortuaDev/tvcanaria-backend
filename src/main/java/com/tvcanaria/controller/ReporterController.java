package com.tvcanaria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tvcanaria.service.ReporterService;

@RestController
@RequestMapping("/api/reporter")
public class ReporterController {

    @Autowired
    private ReporterService reporterService;
}
