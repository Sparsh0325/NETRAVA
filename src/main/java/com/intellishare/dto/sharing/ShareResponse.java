package com.intellishare.dto.sharing;

import com.intellishare.sharing.CaseShare;
import java.time.OffsetDateTime;

public record ShareResponse(Long id,String caseNumber,String caseTitle,String ownerAgency,String recipientAgency,
                            String permission,String status,OffsetDateTime createdAt,OffsetDateTime revokedAt) {
    public static ShareResponse from(CaseShare s) { return new ShareResponse(s.getId(),s.getCaseRecord().getCaseNumber(),s.getCaseRecord().getTitle(),s.getOwnerAgency(),s.getRecipientAgency(),s.getPermission(),s.getStatus(),s.getCreatedAt(),s.getRevokedAt()); }
}
