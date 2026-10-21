package com.intellishare.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record CriminalRecordPageResponse(List<CriminalRecordResponse> content, int page, int size,
                                         long totalElements, int totalPages) {
    public static CriminalRecordPageResponse from(Page<CriminalRecordResponse> result) {
        return new CriminalRecordPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
