package com.routeshare.admin.dto;

import jakarta.validation.constraints.Size;

/** Generic admin action body carrying an optional reason (suspend/activate/resolve, etc.). */
public record AdminActionRequest(@Size(max = 500) String reason) {}
