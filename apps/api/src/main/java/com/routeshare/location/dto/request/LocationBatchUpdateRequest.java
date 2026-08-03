package com.routeshare.location.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LocationBatchUpdateRequest(
    @NotEmpty @Size(max = 100) List<@Valid LocationSampleRequest> samples) {}
