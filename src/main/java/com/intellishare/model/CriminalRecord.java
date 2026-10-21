package com.intellishare.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "criminal_records")
public class CriminalRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "record_number", nullable = false, unique = true, length = 50)
    private String recordNumber;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(length = 150)
    private String alias;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    private Integer age;
    @Column(length = 20)
    private String gender;
    @Column(nullable = false, length = 50)
    private String status;
    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;
    @Column(name = "crime_type", length = 100)
    private String crimeType;
    @Column(name = "last_seen_location", length = 255)
    private String lastSeenLocation;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(name = "is_synthetic_demo", nullable = false)
    private boolean syntheticDemo;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CriminalRecord() { }

    public CriminalRecord(String recordNumber, String name, String alias, LocalDate dateOfBirth, Integer age,
            String gender, String status, String riskLevel, String crimeType, String lastSeenLocation,
            String notes, boolean syntheticDemo) {
        this.recordNumber = recordNumber;
        this.name = name;
        this.alias = alias;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.gender = gender;
        this.status = status;
        this.riskLevel = riskLevel;
        this.crimeType = crimeType;
        this.lastSeenLocation = lastSeenLocation;
        this.notes = notes;
        this.syntheticDemo = syntheticDemo;
    }

    @PrePersist void onCreate() { OffsetDateTime now = OffsetDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public void update(String name, String alias, LocalDate dateOfBirth, Integer age, String gender,
            String status, String riskLevel, String crimeType, String lastSeenLocation, String notes) {
        this.name = name; this.alias = alias; this.dateOfBirth = dateOfBirth; this.age = age;
        this.gender = gender; this.status = status; this.riskLevel = riskLevel; this.crimeType = crimeType;
        this.lastSeenLocation = lastSeenLocation; this.notes = notes;
    }

    public Long getId() { return id; }
    public String getRecordNumber() { return recordNumber; }
    public String getName() { return name; }
    public String getAlias() { return alias; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Integer getAge() { return age; }
    public String getGender() { return gender; }
    public String getStatus() { return status; }
    public String getRiskLevel() { return riskLevel; }
    public String getCrimeType() { return crimeType; }
    public String getLastSeenLocation() { return lastSeenLocation; }
    public String getNotes() { return notes; }
    public boolean isSyntheticDemo() { return syntheticDemo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
