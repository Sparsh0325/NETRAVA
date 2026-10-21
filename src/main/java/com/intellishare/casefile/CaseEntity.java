package com.intellishare.casefile;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="case_entities")
public class CaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="case_id",nullable=false) private InvestigationCase caseRecord;
    @Column(name="entity_type",nullable=false,length=50) private String type;
    @Column(name="entity_value",nullable=false,length=255) private String value;
    @Column(columnDefinition="text") private String details;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    protected CaseEntity(){}
    public CaseEntity(String type,String value,String details){this.type=type;this.value=value;this.details=details;}
    @PrePersist void onCreate(){createdAt=OffsetDateTime.now();}
    void setCaseRecord(InvestigationCase value){caseRecord=value;}
    public Long getId(){return id;} public String getType(){return type;} public String getValue(){return value;} public String getDetails(){return details;}
}
