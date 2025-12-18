package com.mmtext.editorservershare.dto;

import java.util.List;


public class ShareMultipleResponse {
    private Integer totalRecipients;
    private Integer successCount;
    private Integer failureCount;
    private List<ShareResult> results;

    public ShareMultipleResponse() {
    }

    public ShareMultipleResponse(String documentId, List<ShareResult> shareResults, int size, String documentSharingCompleted) {
        this.results = shareResults;
    }

    public Integer getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(Integer totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public List<ShareResult> getResults() {
        return results;
    }

    public void setResults(List<ShareResult> results) {
        this.results = results;
    }
}