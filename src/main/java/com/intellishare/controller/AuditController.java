package com.intellishare.controller;

import com.intellishare.audit.AuditPage;
import com.intellishare.audit.AuditService;
import com.intellishare.model.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrator','Analyst')")
    public AuditPage list(@AuthenticationPrincipal User actor, @RequestParam(defaultValue="0") int page,
                          @RequestParam(defaultValue="50") int size, @RequestParam(required=false) String q) {
        return audit.list(actor, page, size, q);
    }

    @GetMapping("/export.csv")
    @PreAuthorize("hasAnyRole('Administrator','Analyst')")
    public ResponseEntity<String> export(@AuthenticationPrincipal User actor) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=intellishare-audit.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(audit.exportCsv(actor));
    }

    @GetMapping("/verify")
    @PreAuthorize("hasRole('Administrator')")
    public java.util.Map<String, Boolean> verify() { return java.util.Map.of("valid", audit.verify()); }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AuditPage listOwn(@AuthenticationPrincipal User actor, @RequestParam(defaultValue="0") int page,
                             @RequestParam(defaultValue="50") int size) {
        return audit.listOwn(actor, page, size);
    }

    @GetMapping("/me/export.csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> exportOwn(@AuthenticationPrincipal User actor) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=intellishare-my-audit.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(audit.exportOwnCsv(actor));
    }
}
