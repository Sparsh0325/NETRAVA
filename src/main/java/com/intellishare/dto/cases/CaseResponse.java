package com.intellishare.dto.cases;
import com.intellishare.casefile.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
public record CaseResponse(String id, String caseNumber, String title, String description, String type,
    String status, LocalDate date, Location location, String priority, String assignedOfficer,
    Long assignedOfficerId, List<EntityResponse> entities, List<EvidenceResponse> evidence,
    List<TimelineResponse> timeline, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public record Location(Double lat, Double lng, String address, String city) { }
    public record EntityResponse(String id, String type, String value, String details) { }
    public record EvidenceResponse(Long id, String title, String description, String fileUrl, String fileType,
        Long fileSizeBytes, String sha256Hash, String blockchainTxId, String uploadedBy, OffsetDateTime createdAt) { }
    public record TimelineResponse(Long id, String action, String details, String performedBy, OffsetDateTime timestamp) { }
    public static CaseResponse from(InvestigationCase c) {
        return new CaseResponse(c.getCaseNumber(), c.getCaseNumber(), c.getTitle(), c.getDescription(), c.getType(),
            c.getStatus(), c.getIncidentDate(), new Location(c.getLatitude(), c.getLongitude(), c.getAddress(), c.getCity()),
            c.getPriority(), c.getAssignedOfficer()==null?null:c.getAssignedOfficer().getName(),
            c.getAssignedOfficer()==null?null:c.getAssignedOfficer().getId(),
            c.getEntities().stream().map(e->new EntityResponse(e.getId()==null?null:e.getId().toString(),e.getType(),e.getValue(),e.getDetails())).toList(),
            c.getEvidence().stream().map(e->new EvidenceResponse(e.getId(),e.getTitle(),e.getDescription(),e.getFileUrl(),e.getFileType(),e.getFileSizeBytes(),e.getSha256Hash(),e.getBlockchainTxId(),e.getUploadedBy()==null?null:e.getUploadedBy().getName(),e.getCreatedAt())).toList(),
            c.getTimeline().stream().sorted(java.util.Comparator.comparing(CaseTimeline::getTimestamp,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(t->new TimelineResponse(t.getId(),t.getAction(),t.getDetails(),t.getPerformedBy()==null?null:t.getPerformedBy().getName(),t.getTimestamp())).toList(),
            c.getCreatedAt(),c.getUpdatedAt());
    }
}
