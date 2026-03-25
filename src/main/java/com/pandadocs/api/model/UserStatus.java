package com.pandadocs.api.model;

public enum UserStatus {
    ACTIVE, // User is verified and can log in
    UNVERIFIED, // User has registered but not verified email yet
    BANNED // User is banned by an admin
}