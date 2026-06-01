package com.routeshare.identity.domain;

import java.util.UUID;

public record AppUser(
    long appUserId,
    UUID publicId,
    String keycloakSubject,
    String email,
    String phone,
    String displayName,
    String localStatus) {}
