package com.mmtext.editorservershare.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;


public class ShareWithMultipleRequest {

    @NotEmpty(message = "At least one recipient is required")
    @Size(max = 50, message = "Cannot share with more than 50 people at once")
    private List<ShareRecipient> recipients;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;

    public List<ShareRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<ShareRecipient> recipients) {
        this.recipients = recipients;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
