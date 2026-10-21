package com.intellishare.casefile;

import com.intellishare.model.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
public class InvestigationCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "case_number", nullable = false, unique = true, length = 50) private String caseNumber;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "case_type", nullable = false, length = 100) private String type;
    @Column(nullable = false, length = 50) private String status = "Registered";
    @Column(nullable = false, length = 20) private String priority = "Medium";
    @Column(name = "incident_date") private LocalDate incidentDate;
    @Column(length = 255) private String address;
    @Column(length = 100) private String city;
    private Double latitude;
    private Double longitude;
    @Column(nullable = false, length = 100) private String agency;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_officer_id") private User assignedOfficer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id") private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "caseRecord", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id ASC")
    private List<CaseEntity> entities = new ArrayList<>();
    @OneToMany(mappedBy = "caseRecord", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("createdAt DESC")
    private List<CaseEvidence> evidence = new ArrayList<>();
    @OneToMany(mappedBy = "caseRecord", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("timestamp DESC")
    private List<CaseTimeline> timeline = new ArrayList<>();

    protected InvestigationCase() { }
    public InvestigationCase(String caseNumber, String title, String description, String type, String status,
            String priority, LocalDate incidentDate, String address, String city, Double latitude, Double longitude,
            String agency, User assignedOfficer, User createdBy) {
        this.caseNumber=caseNumber; this.title=title; this.description=description; this.type=type; this.status=status;
        this.priority=priority; this.incidentDate=incidentDate; this.address=address; this.city=city;
        this.latitude=latitude; this.longitude=longitude; this.agency=agency;
        this.assignedOfficer=assignedOfficer; this.createdBy=createdBy;
    }
    @PrePersist void onCreate() { OffsetDateTime now=OffsetDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate() { updatedAt=OffsetDateTime.now(); }
    public void update(String title, String description, String type, String status, String priority,
            LocalDate incidentDate, String address, String city, Double latitude, Double longitude, User assignedOfficer) {
        this.title=title; this.description=description; this.type=type; this.status=status; this.priority=priority;
        this.incidentDate=incidentDate; this.address=address; this.city=city; this.latitude=latitude;
        this.longitude=longitude; this.assignedOfficer=assignedOfficer;
    }
    public void replaceEntities(List<CaseEntity> values) {
        entities.clear(); values.forEach(value -> { value.setCaseRecord(this); entities.add(value); });
    }
    public void addEvidence(CaseEvidence value) { value.setCaseRecord(this); evidence.add(value); }
    public void addTimeline(CaseTimeline value) { value.setCaseRecord(this); timeline.add(value); }
    public Long getId(){return id;} public String getCaseNumber(){return caseNumber;} public String getTitle(){return title;}
    public String getDescription(){return description;} public String getType(){return type;} public String getStatus(){return status;}
    public String getPriority(){return priority;} public LocalDate getIncidentDate(){return incidentDate;}
    public String getAddress(){return address;} public String getCity(){return city;} public Double getLatitude(){return latitude;}
    public Double getLongitude(){return longitude;} public String getAgency(){return agency;}
    public User getAssignedOfficer(){return assignedOfficer;} public User getCreatedBy(){return createdBy;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
    public List<CaseEntity> getEntities(){return entities;} public List<CaseEvidence> getEvidence(){return evidence;}
    public List<CaseTimeline> getTimeline(){return timeline;}
}
