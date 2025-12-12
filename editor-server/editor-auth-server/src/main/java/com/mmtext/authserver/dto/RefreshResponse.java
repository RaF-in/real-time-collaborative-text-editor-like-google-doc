package com.mmtext.authserver.dto;

public class RefreshResponse {
    private String accessToken;
    private String tokenType;
    private int expiresIn;

    // This is NOT returned in JSON (security), but used internally to set cookie
    private String newRefreshToken;

    public RefreshResponse() {}

    public RefreshResponse(String accessToken, String tokenType, int expiresIn, String newRefreshToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.newRefreshToken = newRefreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getNewRefreshToken() {
        return newRefreshToken;
    }

    public void setNewRefreshToken(String newRefreshToken) {
        this.newRefreshToken = newRefreshToken;
    }
}