package com.mmtext.editorserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mmtext.editorserver.model.CRDTOperation;

import java.util.Map;

/**
 * WebSocket message from client
 * Server-side fractional positioning: client only sends positions, not fractional index
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientMessage {

    private String type; // SUBSCRIBE, OPERATION, SYNC_REQUEST, UNSUBSCRIBE
    private String docId;
    private String userId;
    private CRDTOperation operation;
    private Map<String, Long> versionVector;

    // For INSERT operations - server generates fractional position
    private String insertAfterPosition;  // Position of character before insertion
    private String insertBeforePosition; // Position of character after insertion

    // Default Constructor
    public ClientMessage() {}

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public CRDTOperation getOperation() {
        return operation;
    }

    public void setOperation(CRDTOperation operation) {
        this.operation = operation;
    }

    public Map<String, Long> getVersionVector() {
        return versionVector;
    }

    public void setVersionVector(Map<String, Long> versionVector) {
        this.versionVector = versionVector;
    }

    public String getInsertAfterPosition() {
        return insertAfterPosition;
    }

    public void setInsertAfterPosition(String insertAfterPosition) {
        this.insertAfterPosition = insertAfterPosition;
    }

    public String getInsertBeforePosition() {
        return insertBeforePosition;
    }

    public void setInsertBeforePosition(String insertBeforePosition) {
        this.insertBeforePosition = insertBeforePosition;
    }
}