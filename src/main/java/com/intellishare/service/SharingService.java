package com.intellishare.service;

import com.intellishare.audit.AuditService;
import com.intellishare.casefile.InvestigationCase;
import com.intellishare.dto.sharing.CreateShareRequest;
import com.intellishare.dto.sharing.ShareResponse;
import com.intellishare.model.User;
import com.intellishare.repository.InvestigationCaseRepository;
import com.intellishare.repository.UserRepository;
import com.intellishare.repository.sharing.CaseShareRepository;
import com.intellishare.sharing.CaseShare;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class SharingService {
    private final CaseShareRepository shares;
    private final InvestigationCaseRepository cases;
    private final UserRepository users;
    private final AuditService audit;
    public SharingService(CaseShareRepository shares,InvestigationCaseRepository cases,UserRepository users,AuditService audit){this.shares=shares;this.cases=cases;this.users=users;this.audit=audit;}

    @Transactional(readOnly=true)
    public List<ShareResponse> list(User actor){
        List<CaseShare> rows="Administrator".equals(actor.getRole())?shares.findTop500ByOrderByCreatedAtDesc():shares.findTop500ByOwnerAgencyOrRecipientAgencyOrderByCreatedAtDesc(actor.getAgency(),actor.getAgency());
        return rows.stream().map(ShareResponse::from).toList();
    }

    @Transactional
    public ShareResponse create(User actor,String caseKey,CreateShareRequest request){
        InvestigationCase record=findCase(caseKey);
        if(!"Administrator".equals(actor.getRole())&&!record.getAgency().equals(actor.getAgency())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found");
        String requestedAgency=request.recipientAgency().trim();
        if(requestedAgency.equalsIgnoreCase(record.getAgency())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A case cannot be shared with its own agency");
        List<User> recipientUsers=users.findTop1ByAgencyIgnoreCaseAndActiveTrueOrderByIdAsc(requestedAgency);
        if(recipientUsers.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Recipient agency has no active users");
        String recipient=recipientUsers.getFirst().getAgency();
        String permission=request.permission()==null||request.permission().isBlank()?"READ":request.permission();
        CaseShare share=shares.findByCaseRecordIdAndRecipientAgency(record.getId(),recipient)
                .map(existing->{existing.reactivate(permission,actor);return existing;})
                .orElseGet(()->new CaseShare(record,recipient,permission,actor));
        share=shares.saveAndFlush(share);
        audit.record(actor,"CASE_SHARE_GRANT","case:"+record.getCaseNumber(),"Shared with "+recipient+" using "+permission+" permission");
        return ShareResponse.from(share);
    }

    @Transactional
    public void revoke(User actor,long shareId){
        CaseShare share=shares.findById(shareId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Share not found"));
        if(!"Administrator".equals(actor.getRole())&&!share.getOwnerAgency().equals(actor.getAgency())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Share not found");
        if("REVOKED".equals(share.getStatus())) return;
        share.revoke();
        audit.record(actor,"CASE_SHARE_REVOKE","case:"+share.getCaseRecord().getCaseNumber(),"Access revoked for "+share.getRecipientAgency());
    }

    private InvestigationCase findCase(String key){
        InvestigationCase record=cases.findByCaseNumberIgnoreCase(key).orElse(null);
        if(record==null) try{record=cases.findById(Long.valueOf(key)).orElse(null);}catch(NumberFormatException ignored){ }
        if(record==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found");
        return record;
    }
}
