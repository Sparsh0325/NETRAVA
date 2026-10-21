package com.intellishare.casefile;
import com.intellishare.model.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="case_timelines")
public class CaseTimeline {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="case_id",nullable=false) private InvestigationCase caseRecord;
    @Column(nullable=false,length=255) private String action;
    @Column(columnDefinition="text") private String details;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="performed_by_id") private User performedBy;
    @Column(name="timestamp",nullable=false,updatable=false) private OffsetDateTime timestamp;
    protected CaseTimeline(){}
    public CaseTimeline(String action,String details,User user){this.action=action;this.details=details;performedBy=user;}
    @PrePersist void onCreate(){timestamp=OffsetDateTime.now();}
    void setCaseRecord(InvestigationCase value){caseRecord=value;}
    public Long getId(){return id;} public String getAction(){return action;} public String getDetails(){return details;}
    public User getPerformedBy(){return performedBy;} public OffsetDateTime getTimestamp(){return timestamp;}
}
