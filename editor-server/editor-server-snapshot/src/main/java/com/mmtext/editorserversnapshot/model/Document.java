package com.mmtext.editorserversnapshot.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

@Entity
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false, unique = true, length = 255)
    private String docId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "allow_access_requests", nullable = false)
    private Boolean allowAccessRequests = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocumentSnapshot> snapshots = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VersionVector> versionVectors = new ArrayList<>();

    public Document() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Document(String docId, String title, String ownerId) {
        this();
        this.docId = docId;
        this.title = title;
        this.ownerId = ownerId;
    }

    public Document(String docId, String title, String ownerId, Boolean allowAccessRequests) {
        this();
        this.docId = docId;
        this.title = title;
        this.ownerId = ownerId;
        this.allowAccessRequests = allowAccessRequests != null ? allowAccessRequests : true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Boolean getAllowAccessRequests() {
        return allowAccessRequests;
    }

    public void setAllowAccessRequests(Boolean allowAccessRequests) {
        this.allowAccessRequests = allowAccessRequests != null ? allowAccessRequests : true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DocumentSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<DocumentSnapshot> snapshots) {
        this.snapshots = snapshots;
        if (snapshots != null) {
            snapshots.forEach(snapshot -> snapshot.setDocument(this));
        }
    }

    public List<VersionVector> getVersionVectors() {
        return versionVectors;
    }

    public void setVersionVectors(List<VersionVector> versionVectors) {
        this.versionVectors = versionVectors;
        if (versionVectors != null) {
            versionVectors.forEach(vector -> vector.setDocument(this));
        }
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", docId='" + docId + '\'' +
                ", title='" + title + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", allowAccessRequests=" + allowAccessRequests +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}