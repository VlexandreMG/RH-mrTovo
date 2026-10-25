package com.mrtovo.backend.controller;

import com.mrtovo.backend.service.DashboardService;
import com.mrtovo.backend.dto.DashboardDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bloc3/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardDto tableauDeBord() {
        return dashboardService.tableauDeBord();
    }
}
