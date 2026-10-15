package com.reglog.model;

import java.sql.Timestamp;

public class UserSession {
    private Long sessionId;
    private Long userId;
    private String tokenIdentifier;
    private Timestamp loginTime;
    private Timestamp expiryTime;
    private boolean revocationStatus;

    public UserSession() {}

    public UserSession(Long sessionId, Long userId, String tokenIdentifier, Timestamp loginTime, Timestamp expiryTime, boolean revocationStatus) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.tokenIdentifier = tokenIdentifier;
        this.loginTime = loginTime;
        this.expiryTime = expiryTime;
        this.revocationStatus = revocationStatus;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTokenIdentifier() { return tokenIdentifier; }
    public void setTokenIdentifier(String tokenIdentifier) { this.tokenIdentifier = tokenIdentifier; }

    public Timestamp getLoginTime() { return loginTime; }
    public void setLoginTime(Timestamp loginTime) { this.loginTime = loginTime; }

    public Timestamp getExpiryTime() { return expiryTime; }
    public void setExpiryTime(Timestamp expiryTime) { this.expiryTime = expiryTime; }

    public boolean isRevocationStatus() { return revocationStatus; }
    public void setRevocationStatus(boolean revocationStatus) { this.revocationStatus = revocationStatus; }
}
