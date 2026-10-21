package com.intellishare.sharing;

import com.intellishare.casefile.InvestigationCase;
import com.intellishare.model.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="case_shares", uniqueConstraints=@UniqueConstraint(name="uq_case_share_target",columnNames={"case_id","recipient_agency"}))
public class CaseShare {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="case_id",nullable=false) private InvestigationCase caseRecord;
    @Column(name="owner_agency",nullable=false,length=100) private String ownerAgency;
    @Column(name="recipient_agency",nullable=false,length=100) private String recipientAgency;
    @Column(nullable=false,length=10) private String permission;
    @Column(nullable=false,length=10) private String status;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by_id") private User createdBy;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="revoked_at") private OffsetDateTime revokedAt;

    protected CaseShare() { }
    public CaseShare(InvestigationCase record,String recipient,String permission,User actor) {
        this.caseRecord=record;this.ownerAgency=record.getAgency();this.recipientAgency=recipient;this.permission=permission;this.status="ACTIVE";this.createdBy=actor;this.createdAt=OffsetDateTime.now();
    }
    public void reactivate(String permission,User actor) { this.permission=permission;this.createdBy=actor;this.status="ACTIVE";this.revokedAt=null;this.createdAt=OffsetDateTime.now(); }
    public void revoke() { this.status="REVOKED";this.revokedAt=OffsetDateTime.now(); }
    public Long getId(){return id;} public InvestigationCase getCaseRecord(){return caseRecord;} public String getOwnerAgency(){return ownerAgency;}
    public String getRecipientAgency(){return recipientAgency;} public String getPermission(){return permission;} public String getStatus(){return status;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getRevokedAt(){return revokedAt;}
}
