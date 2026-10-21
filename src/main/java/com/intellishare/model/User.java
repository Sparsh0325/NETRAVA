package com.intellishare.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "badge_number", nullable = false, unique = true, length = 50)
    private String badgeNumber;

    @Column(nullable = false, length = 100)
    private String agency;

    @Column(nullable = false, length = 50)
    private String role = "Read-Only";

    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String phone;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }

    protected User() { }

    public User(String name, String email, String passwordHash, String badgeNumber, String agency,
                String department, String phone) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.badgeNumber = badgeNumber;
        this.agency = agency;
        this.department = department;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getBadgeNumber() { return badgeNumber; }
    public String getAgency() { return agency; }
    public String getRole() { return role; }
    public String getDepartment() { return department; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isActive() { return active; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
