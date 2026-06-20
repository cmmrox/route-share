package com.routeshare.admin.dto;

public record AdminDriverApplicationResponse(
    long driverProfileId, long appUserId, String displayName, String verificationStatus) {}
