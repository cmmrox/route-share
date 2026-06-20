package com.routeshare.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateRolesRequest(@NotNull Set<String> roles) {}
