package com.mmtext.authserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")

public class AppConfig {
    private Auth auth = new Auth();
    private Frontend frontend = new Frontend();

    
    public static class Auth {
        private RateLimit rateLimit = new RateLimit();
        private Password password = new Password();
        private AccountLockout accountLockout = new AccountLockout();

        public RateLimit getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
        }

        public Password getPassword() {
            return password;
        }

        public void setPassword(Password password) {
            this.password = password;
        }

        public AccountLockout getAccountLockout() {
            return accountLockout;
        }

        public void setAccountLockout(AccountLockout accountLockout) {
            this.accountLockout = accountLockout;
        }
    }

    
    public static class RateLimit {
        private int loginAttempts;
        private int windowMinutes;

        public int getLoginAttempts() {
            return loginAttempts;
        }

        public void setLoginAttempts(int loginAttempts) {
            this.loginAttempts = loginAttempts;
        }

        public int getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(int windowMinutes) {
            this.windowMinutes = windowMinutes;
        }
    }

    
    public static class Password {
        private int minLength;
        private boolean requireUppercase;
        private boolean requireLowercase;
        private boolean requireDigit;
        private boolean requireSpecial;

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public boolean isRequireUppercase() {
            return requireUppercase;
        }

        public void setRequireUppercase(boolean requireUppercase) {
            this.requireUppercase = requireUppercase;
        }

        public boolean isRequireLowercase() {
            return requireLowercase;
        }

        public void setRequireLowercase(boolean requireLowercase) {
            this.requireLowercase = requireLowercase;
        }

        public boolean isRequireDigit() {
            return requireDigit;
        }

        public void setRequireDigit(boolean requireDigit) {
            this.requireDigit = requireDigit;
        }

        public boolean isRequireSpecial() {
            return requireSpecial;
        }

        public void setRequireSpecial(boolean requireSpecial) {
            this.requireSpecial = requireSpecial;
        }
    }

    
    public static class AccountLockout {
        private int maxAttempts;
        private int lockoutDurationMinutes;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getLockoutDurationMinutes() {
            return lockoutDurationMinutes;
        }

        public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
            this.lockoutDurationMinutes = lockoutDurationMinutes;
        }
    }

    
    public static class Frontend {
        private String url;
        private String oauthRedirectPath;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getOauthRedirectPath() {
            return oauthRedirectPath;
        }

        public void setOauthRedirectPath(String oauthRedirectPath) {
            this.oauthRedirectPath = oauthRedirectPath;
        }
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public void setFrontend(Frontend frontend) {
        this.frontend = frontend;
    }
}