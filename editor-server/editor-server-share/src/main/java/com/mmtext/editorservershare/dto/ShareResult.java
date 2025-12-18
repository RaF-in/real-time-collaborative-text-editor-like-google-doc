package com.mmtext.editorservershare.dto;

public class ShareResult {
    private String email;
    private Boolean success;
    private String message;
    private DocumentPermissionResponse permission;

    public ShareResult(String email, boolean b, String userNotFound) {
        this.email = email;
        this.success = b;
        this.message = userNotFound;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DocumentPermissionResponse getPermission() {
        return permission;
    }

    public void setPermission(DocumentPermissionResponse permission) {
        this.permission = permission;
    }

    public static class Builder {
        private String email;
        private Boolean success;
        private String message;
        private DocumentPermissionResponse permission;

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder permission(DocumentPermissionResponse permission) {
            this.permission = permission;
            return this;
        }

        public ShareResult build() {
            ShareResult result = new ShareResult(email, success != null ? success : false, message);
            result.permission = this.permission;
            return result;
        }
    }
}