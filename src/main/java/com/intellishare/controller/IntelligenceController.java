package com.intellishare.controller;

import com.intellishare.dto.intelligence.IntelligenceRequest;
import com.intellishare.dto.intelligence.IntelligenceResponse;
import com.intellishare.model.User;
import com.intellishare.service.IntelligenceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {
    private final IntelligenceService service;
    public IntelligenceController(IntelligenceService service) { this.service = service; }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('Administrator','Officer','Analyst')")
    public IntelligenceResponse analyze(@AuthenticationPrincipal User actor, @Valid @RequestBody IntelligenceRequest request) {
        return service.analyze(actor, request);
    }
}
