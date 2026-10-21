package com.intellishare.service;

import com.intellishare.dto.CriminalRecordPageResponse;
import com.intellishare.dto.CriminalRecordRequest;
import com.intellishare.dto.CriminalRecordResponse;
import com.intellishare.model.CriminalRecord;
import com.intellishare.model.User;
import com.intellishare.audit.AuditService;
import com.intellishare.repository.CriminalRecordRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

@Service
public class CriminalRecordService {
    private final CriminalRecordRepository records;
    private final AuditService audit;

    public CriminalRecordService(CriminalRecordRepository records, AuditService audit) { this.records = records; this.audit = audit; }

    @Transactional(readOnly = true)
    public CriminalRecordPageResponse list(String query, String status, String riskLevel, int page, int size,
                                           String sort, String direction, Boolean syntheticDemo) {
        if (page < 0 || size < 1 || size > 100) throw badRequest("Page must be non-negative and size must be 1-100");
        String property = switch (sort == null ? "createdAt" : sort) {
            case "recordNumber" -> "recordNumber";
            case "name" -> "name";
            case "status" -> "status";
            case "riskLevel" -> "riskLevel";
            case "createdAt" -> "createdAt";
            default -> throw badRequest("Unsupported sort field");
        };
        Sort.Direction order;
        try { order = Sort.Direction.fromString(direction == null ? "desc" : direction); }
        catch (IllegalArgumentException ex) { throw badRequest("Sort direction must be asc or desc"); }

        var spec = (org.springframework.data.jpa.domain.Specification<CriminalRecord>) (root, criteria, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), status));
            if (riskLevel != null && !riskLevel.isBlank()) predicates.add(cb.equal(root.get("riskLevel"), riskLevel));
            if (syntheticDemo != null) predicates.add(cb.equal(root.get("syntheticDemo"), syntheticDemo));
            if (query != null && !query.isBlank()) {
                String needle = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("recordNumber")), needle),
                        cb.like(cb.lower(root.get("name")), needle),
                        cb.like(cb.lower(cb.coalesce(root.get("alias"), "")), needle),
                        cb.like(cb.lower(cb.coalesce(root.get("crimeType"), "")), needle),
                        cb.like(cb.lower(cb.coalesce(root.get("lastSeenLocation"), "")), needle)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = records.findAll(spec, PageRequest.of(page, size, Sort.by(order, property))).map(CriminalRecordResponse::from);
        return CriminalRecordPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CriminalRecordResponse get(String recordNumber) { return CriminalRecordResponse.from(find(recordNumber)); }

    @Transactional
    public CriminalRecordResponse create(User actor, CriminalRecordRequest request) {
        String number = "CR-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CriminalRecord record = new CriminalRecord(number, request.name().trim(), clean(request.alias()),
                request.dateOfBirth(), request.age(), clean(request.gender()), request.status(), request.riskLevel(),
                clean(request.crimeType()), clean(request.lastSeenLocation()), clean(request.notes()), false);
        try { record = records.saveAndFlush(record); audit.record(actor,"CRIMINAL_RECORD_CREATE","criminal-record:"+record.getRecordNumber(),"Registry record created"); return CriminalRecordResponse.from(record); }
        catch (DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Record could not be saved because it conflicts with an existing record"); }
    }

    @Transactional
    public CriminalRecordResponse update(User actor, String recordNumber, CriminalRecordRequest request) {
        CriminalRecord record = find(recordNumber);
        record.update(request.name().trim(), clean(request.alias()), request.dateOfBirth(), request.age(),
                clean(request.gender()), request.status(), request.riskLevel(), clean(request.crimeType()),
                clean(request.lastSeenLocation()), clean(request.notes()));
        audit.record(actor,"CRIMINAL_RECORD_UPDATE","criminal-record:"+record.getRecordNumber(),"Registry record updated");
        return CriminalRecordResponse.from(record);
    }

    @Transactional
    public void delete(User actor, String recordNumber) { CriminalRecord record=find(recordNumber); records.delete(record); audit.record(actor,"CRIMINAL_RECORD_DELETE","criminal-record:"+record.getRecordNumber(),"Registry record deleted"); }

    private CriminalRecord find(String recordNumber) {
        return records.findOne((root, query, cb) -> cb.equal(root.get("recordNumber"), recordNumber))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Criminal record not found"));
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
