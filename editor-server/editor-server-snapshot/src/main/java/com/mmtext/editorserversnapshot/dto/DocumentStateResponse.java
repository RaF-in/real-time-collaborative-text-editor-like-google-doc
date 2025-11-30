package com.mmtext.editorserversnapshot.dto;



import com.mmtext.editorserversnapshot.model.DocumentSnapshot;

import java.util.List;
import java.util.Map;

/**
 * Response containing document state for new clients
 */
public class DocumentStateResponse {

    private String docId;
    private List<DocumentSnapshot> snapshot;
    private Map<String, Long> versionVector;
    private String content;

    // Default Constructor
    public DocumentStateResponse() {}

    // Parameterized Constructor
    public DocumentStateResponse(String docId, List<DocumentSnapshot> snapshot,
                                 Map<String, Long> versionVector, String content) {
        this.docId = docId;
        this.snapshot = snapshot;
        this.versionVector = versionVector;
        this.content = content;
    }

    // Getters and Setters
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public List<DocumentSnapshot> getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(List<DocumentSnapshot> snapshot) {
        this.snapshot = snapshot;
    }

    public Map<String, Long> getVersionVector() {
        return versionVector;
    }

    public void setVersionVector(Map<String, Long> versionVector) {
        this.versionVector = versionVector;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

