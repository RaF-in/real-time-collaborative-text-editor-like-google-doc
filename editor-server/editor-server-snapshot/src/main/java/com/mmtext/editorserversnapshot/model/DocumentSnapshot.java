package com.mmtext.editorserversnapshot.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "document_snapshots", indexes = {
        @Index(name = "idx_doc_position", columnList = "docId,fractionalPosition")
})
public class DocumentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String docId;

    @Column(nullable = false, length = 500)
    private String fractionalPosition;

    @Column(length = 10)
    private String character;

    @Column(nullable = false)
    private String serverId;

    @Column(nullable = false)
    private Long serverSeqNum;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean active = true;

    // Default Constructor
    public DocumentSnapshot() {
        this.createdAt = Instant.now();
    }

    // Parameterized Constructor
    public DocumentSnapshot(String docId, String fractionalPosition,
                            String character, String serverId, Long serverSeqNum) {
        this.docId = docId;
        this.fractionalPosition = fractionalPosition;
        this.character = character;
        this.serverId = serverId;
        this.serverSeqNum = serverSeqNum;
        this.createdAt = Instant.now();
        this.active = true;
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

    public String getFractionalPosition() {
        return fractionalPosition;
    }

    public void setFractionalPosition(String fractionalPosition) {
        this.fractionalPosition = fractionalPosition;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Long getServerSeqNum() {
        return serverSeqNum;
    }

    public void setServerSeqNum(Long serverSeqNum) {
        this.serverSeqNum = serverSeqNum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
