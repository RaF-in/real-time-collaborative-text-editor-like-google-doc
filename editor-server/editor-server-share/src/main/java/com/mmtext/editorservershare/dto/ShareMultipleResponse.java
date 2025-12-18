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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer totalRecipients;
        private Integer successCount;
        private Integer failureCount;
        private List<ShareResult> results;

        public Builder totalRecipients(Integer totalRecipients) {
            this.totalRecipients = totalRecipients;
            return this;
        }

        public Builder successCount(Integer successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failureCount(Integer failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public Builder results(List<ShareResult> results) {
            this.results = results;
            return this;
        }

        public ShareMultipleResponse build() {
            ShareMultipleResponse response = new ShareMultipleResponse();
            response.totalRecipients = this.totalRecipients;
            response.successCount = this.successCount;
            response.failureCount = this.failureCount;
            response.results = this.results;
            return response;
        }
    }
}