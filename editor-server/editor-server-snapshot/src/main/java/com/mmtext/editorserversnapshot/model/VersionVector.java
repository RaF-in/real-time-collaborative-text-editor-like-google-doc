package com.mmtext.editorserversnapshot.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "version_vectors", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"docId", "serverId"})
})
public class VersionVector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String docId;

    @Column(nullable = false)
    private String serverId;

    @Column(nullable = false)
    private Long sequenceNumber;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = true)
    private Document document;

    // Default Constructor
    public VersionVector() {
        this.updatedAt = Instant.now();
    }

    // Parameterized Constructor
    public VersionVector(String docId, String serverId, Long sequenceNumber) {
        this.docId = docId;
        this.serverId = serverId;
        this.sequenceNumber = sequenceNumber;
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

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
