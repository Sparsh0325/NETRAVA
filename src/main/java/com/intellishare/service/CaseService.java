package com.intellishare.service;

import com.intellishare.casefile.*;
import com.intellishare.dto.cases.*;
import com.intellishare.model.User;
import com.intellishare.audit.AuditService;
import com.intellishare.sharing.CaseShare;
import com.intellishare.repository.sharing.CaseShareRepository;
import com.intellishare.repository.InvestigationCaseRepository;
import com.intellishare.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class CaseService {
    private final InvestigationCaseRepository cases;
    private final UserRepository users;
    private final AuditService audit;
    private final CaseShareRepository shares;

    public CaseService(InvestigationCaseRepository cases, UserRepository users, AuditService audit, CaseShareRepository shares) { this.cases=cases; this.users=users; this.audit=audit; this.shares=shares; }

    @Transactional
    public CasePageResponse list(User actor, String query, String status, String priority, String type, String city,
                                 int page, int size, String sort, String direction) {
        if (page < 0 || size < 1 || size > 100) throw badRequest("Page must be non-negative and size must be 1-100");
        String property = switch (sort == null ? "createdAt" : sort) {
            case "caseNumber" -> "caseNumber"; case "title" -> "title"; case "status" -> "status";
            case "priority" -> "priority"; case "date", "incidentDate" -> "incidentDate";
            case "city" -> "city"; case "createdAt" -> "createdAt"; case "updatedAt" -> "updatedAt";
            default -> throw badRequest("Unsupported sort field");
        };
        Sort.Direction order;
        try { order=Sort.Direction.fromString(direction == null ? "DESC" : direction); }
        catch (IllegalArgumentException ex) { throw badRequest("Sort direction must be asc or desc"); }
        Specification<InvestigationCase> spec = (root, criteria, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!"Administrator".equals(actor.getRole())) {
                var shared=criteria.subquery(Long.class);
                var share=shared.from(CaseShare.class);
                shared.select(share.get("id")).where(cb.equal(share.get("caseRecord").get("id"),root.get("id")),
                        cb.equal(share.get("recipientAgency"),actor.getAgency()),cb.equal(share.get("status"),"ACTIVE"));
                predicates.add(cb.or(cb.equal(root.get("agency"), actor.getAgency()),cb.exists(shared)));
            }
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), status));
            if (priority != null && !priority.isBlank()) predicates.add(cb.equal(root.get("priority"), priority));
            if (type != null && !type.isBlank()) predicates.add(cb.equal(cb.lower(root.get("type")), type.toLowerCase(Locale.ROOT)));
            if (city != null && !city.isBlank()) predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase(Locale.ROOT)));
            if (query != null && !query.isBlank()) {
                var officer = root.join("assignedOfficer", JoinType.LEFT);
                String needle = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("caseNumber")), needle),
                        cb.like(cb.lower(root.get("title")), needle), cb.like(cb.lower(cb.coalesce(root.get("city"), "")), needle),
                        cb.like(cb.lower(cb.coalesce(officer.get("name"), "")), needle)));
                criteria.distinct(true);
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<InvestigationCase> pageResult=cases.findAll(spec, PageRequest.of(page,size,Sort.by(order,property)));
        if (!"Administrator".equals(actor.getRole()) && pageResult.getContent().stream().anyMatch(c->!actor.getAgency().equals(c.getAgency())))
            audit.record(actor,"SHARED_CASE_LIST_ACCESS","agency:"+actor.getAgency(),"Listed cases shared with this agency");
        return CasePageResponse.from(pageResult.map(CaseResponse::from));
    }

    @Transactional
    public CaseResponse get(User actor, String id) { InvestigationCase record=findVisible(actor,id); if(!"Administrator".equals(actor.getRole())&&!actor.getAgency().equals(record.getAgency())) audit.record(actor,"SHARED_CASE_ACCESS","case:"+record.getCaseNumber(),"Read case through agency share"); return CaseResponse.from(record); }

    @Transactional
    public CaseResponse create(User actor, CreateCaseRequest request) {
        String number=request.caseNumber().trim();
        if(cases.existsByCaseNumberIgnoreCase(number)) throw conflict("Case number already exists");
        User assignee=resolveAssignee(actor,request.assignedOfficerId());
        InvestigationCase record=new InvestigationCase(number,request.title().trim(),request.description(),request.type().trim(),
                valueOr(request.status(),"Registered"),valueOr(request.priority(),"Medium"),request.incidentDate(),clean(request.address()),
                clean(request.city()),request.latitude(),request.longitude(),actor.getAgency(),assignee,actor);
        record.replaceEntities(toEntities(request.entities()));
        record.addTimeline(new CaseTimeline("Case created", "Case registered", actor));
        record = cases.saveAndFlush(record);
        audit.record(actor, "CASE_CREATE", "case:" + record.getCaseNumber(), "Case created");
        return CaseResponse.from(record);
    }

    @Transactional
    public CaseResponse update(User actor, String id, UpdateCaseRequest request) {
        InvestigationCase record=findVisible(actor,id);
        requireEditAccess(actor,record);
        String oldStatus=record.getStatus();
        User assignee=resolveAssignee(actor,request.assignedOfficerId());
        record.update(request.title().trim(),request.description(),request.type().trim(),request.status(),request.priority(),
                request.incidentDate(),clean(request.address()),clean(request.city()),request.latitude(),request.longitude(),assignee);
        if(request.entities()!=null) record.replaceEntities(toEntities(request.entities()));
        if(!oldStatus.equals(request.status())) record.addTimeline(new CaseTimeline("Status changed",oldStatus+" to "+request.status(),actor));
        cases.flush();
        audit.record(actor, "CASE_UPDATE", "case:" + record.getCaseNumber(), "Case updated");
        return CaseResponse.from(record);
    }

    @Transactional
    public void delete(User actor,String id) { InvestigationCase record=findVisible(actor,id); requireEditAccess(actor,record); String number=record.getCaseNumber(); cases.delete(record); audit.record(actor,"CASE_DELETE","case:"+number,"Case deleted"); }

    @Transactional
    public CaseResponse addEvidence(User actor,String id,CreateEvidenceRequest request) {
        InvestigationCase record=findVisible(actor,id);
        requireEditAccess(actor,record);
        record.addEvidence(new CaseEvidence(request.title().trim(),request.description(),clean(request.fileUrl()),clean(request.fileType()),
                request.fileSizeBytes(),request.sha256Hash()==null?null:request.sha256Hash().toLowerCase(Locale.ROOT),
                clean(request.blockchainTxId()),actor));
        record.addTimeline(new CaseTimeline("Evidence added",request.title().trim(),actor));
        cases.flush();
        audit.record(actor, "CASE_EVIDENCE_ADD", "case:" + record.getCaseNumber(), "Evidence metadata added: " + request.title().trim());
        return CaseResponse.from(record);
    }

    @Transactional
    public CaseResponse addTimeline(User actor,String id,CreateTimelineRequest request) {
        InvestigationCase record=findVisible(actor,id);
        requireEditAccess(actor,record);
        record.addTimeline(new CaseTimeline(request.action().trim(),request.details(),actor));
        cases.flush();
        audit.record(actor, "CASE_TIMELINE_ADD", "case:" + record.getCaseNumber(), "Timeline entry added: " + request.action().trim());
        return CaseResponse.from(record);
    }

    private InvestigationCase findVisible(User actor,String id) {
        InvestigationCase record=cases.findByCaseNumberIgnoreCase(id).orElseGet(()-> {
            try { return cases.findById(Long.valueOf(id)).orElse(null); } catch(NumberFormatException ignored) { return null; }
        });
        if(record==null || (!"Administrator".equals(actor.getRole()) && !record.getAgency().equals(actor.getAgency())
                && !shares.existsByCaseRecordIdAndRecipientAgencyAndStatus(record.getId(),actor.getAgency(),"ACTIVE")))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found");
        return record;
    }

    private void requireEditAccess(User actor,InvestigationCase record) {
        if("Administrator".equals(actor.getRole()) || record.getAgency().equals(actor.getAgency())) return;
        if(!shares.existsByCaseRecordIdAndRecipientAgencyAndPermissionAndStatus(record.getId(),actor.getAgency(),"EDIT","ACTIVE"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This agency share grants read-only access");
    }

    private User resolveAssignee(User actor,Long id) {
        if(id==null) return null;
        User target=users.findById(id).filter(User::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Assigned officer not found"));
        if(!"Administrator".equals(actor.getRole()) && !target.getAgency().equals(actor.getAgency()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Assigned officer must belong to your agency");
        return target;
    }
    private List<CaseEntity> toEntities(List<EntityInput> inputs) {
        if(inputs==null) return List.of();
        return inputs.stream().map(e->new CaseEntity(e.type(),e.value().trim(),clean(e.details()))).toList();
    }
    private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
    private String valueOr(String v,String fallback){return v==null||v.isBlank()?fallback:v;}
    private ResponseStatusException badRequest(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
