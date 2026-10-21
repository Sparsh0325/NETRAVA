package com.intellishare.dto.cases;
import org.springframework.data.domain.Page;
import java.util.List;
public record CasePageResponse(List<CaseResponse> content, int page, int size, long totalElements, int totalPages) {
    public static CasePageResponse from(Page<CaseResponse> page) {
        return new CasePageResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
