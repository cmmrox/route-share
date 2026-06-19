package com.routeshare.admin.dto;

public record AdminUserResponse(
    long appUserId,
    String publicId,
    String email,
    String phone,
    String displayName,
    String status) {}
