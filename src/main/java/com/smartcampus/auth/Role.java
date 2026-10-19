package com.smartcampus.auth;

public enum Role {
    STUDENT("usms.ac.ma"),
    TEACHER("usms.ma"),
    ADMIN("ensakh.ma");

    private final String emailDomain;

    Role(String emailDomain) {
        this.emailDomain = emailDomain;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public boolean acceptsEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.toLowerCase().endsWith("@" + emailDomain);
    }
}