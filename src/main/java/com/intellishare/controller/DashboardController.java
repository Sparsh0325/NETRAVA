package com.intellishare.controller;
import com.intellishare.dto.dashboard.DashboardStatsResponse;
import com.intellishare.dto.dashboard.CaseAnalyticsResponse;
import com.intellishare.model.User;
import com.intellishare.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    private final com.intellishare.service.CaseAnalyticsService analytics;
    public DashboardController(DashboardService dashboard, com.intellishare.service.CaseAnalyticsService analytics){this.dashboard=dashboard;this.analytics=analytics;}
    @GetMapping("/stats") public DashboardStatsResponse stats(@AuthenticationPrincipal User actor){return dashboard.stats(actor);}
    @GetMapping("/analytics") public CaseAnalyticsResponse analytics(@AuthenticationPrincipal User actor){return analytics.analytics(actor);}
}
