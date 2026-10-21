package com.intellishare.controller;

import com.intellishare.dto.ingestion.IngestionResponse;
import com.intellishare.model.User;
import com.intellishare.service.IngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {
    private final IngestionService ingestion;
    public IngestionController(IngestionService ingestion) { this.ingestion=ingestion; }

    @PostMapping(value="/cases.csv",consumes="multipart/form-data")
    @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public ResponseEntity<IngestionResponse> importCsv(@AuthenticationPrincipal User actor,@RequestPart("file") MultipartFile file) {
        IngestionResponse result=ingestion.importCases(actor,file);
        return ResponseEntity.status(result.status().equals("SUCCEEDED")?HttpStatus.CREATED:HttpStatus.UNPROCESSABLE_ENTITY).body(result);
    }
}
