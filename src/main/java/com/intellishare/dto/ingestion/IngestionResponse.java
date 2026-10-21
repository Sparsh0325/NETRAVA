package com.intellishare.dto.ingestion;

import java.util.List;
import java.util.UUID;

public record IngestionResponse(UUID jobId, String status, int processedRows, List<String> errors) { }
