package com.intellishare.controller;

import com.intellishare.dto.CriminalRecordPageResponse;
import com.intellishare.dto.CriminalRecordRequest;
import com.intellishare.dto.CriminalRecordResponse;
import com.intellishare.service.CriminalRecordService;
import com.intellishare.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/criminal-records")
public class CriminalRecordController {
    private final CriminalRecordService service;
    public CriminalRecordController(CriminalRecordService service) { this.service = service; }

    @GetMapping
    public CriminalRecordPageResponse list(@RequestParam(required = false) String q,
            @RequestParam(required = false) String status, @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Boolean syntheticDemo, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return service.list(q, status, riskLevel, page, size, sort, direction, syntheticDemo);
    }

    @GetMapping("/{recordNumber}")
    public CriminalRecordResponse get(@PathVariable String recordNumber) { return service.get(recordNumber); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public CriminalRecordResponse create(@AuthenticationPrincipal User actor, @Valid @RequestBody CriminalRecordRequest request) { return service.create(actor, request); }

    @PutMapping("/{recordNumber}") @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public CriminalRecordResponse update(@AuthenticationPrincipal User actor, @PathVariable String recordNumber, @Valid @RequestBody CriminalRecordRequest request) {
        return service.update(actor, recordNumber, request);
    }

    @DeleteMapping("/{recordNumber}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('Administrator')")
    public void delete(@AuthenticationPrincipal User actor, @PathVariable String recordNumber) { service.delete(actor, recordNumber); }
}
