package com.routeshare.common.security;

import java.util.Set;

public record CurrentUser(
    String subject, String email, String phone, String displayName, Set<String> roles) {}
