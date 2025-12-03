package com.mmtext.editorservermain.model;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "crdt_operations", indexes = {
        @Index(name = "idx_doc_seq", columnList = "docId,serverSeqNum"),
        @Index(name = "idx_doc_server_seq", columnList = "docId,serverId,serverSeqNum")
})
public class CRDTOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String docId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String serverId;

    @Column(nullable = false)
    private String operationType; // "INSERT" or "DELETE"

    @Column(length = 10)
    private String character;

    @Column(nullable = false, length = 500)
    private String fractionalPosition;

    @Column(nullable = false)
    private Long serverSeqNum;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Boolean processed = false;

    // Default Constructor
    public CRDTOperation() {
        this.timestamp = Instant.now();
    }

    // Parameterized Constructor
    public CRDTOperation(String docId, String userId, String serverId,
                         String operationType, String character,
                         String fractionalPosition, Long serverSeqNum) {
        this.docId = docId;
        this.userId = userId;
        this.serverId = serverId;
        this.operationType = operationType;
        this.character = character;
        this.fractionalPosition = fractionalPosition;
        this.serverSeqNum = serverSeqNum;
        this.timestamp = Instant.now();
        this.processed = false;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public String getFractionalPosition() {
        return fractionalPosition;
    }

    public void setFractionalPosition(String fractionalPosition) {
        this.fractionalPosition = fractionalPosition;
    }

    public Long getServerSeqNum() {
        return serverSeqNum;
    }

    public void setServerSeqNum(Long serverSeqNum) {
        this.serverSeqNum = serverSeqNum;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }
}