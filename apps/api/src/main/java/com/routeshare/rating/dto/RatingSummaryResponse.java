package com.routeshare.rating.dto;

import java.util.List;

public record RatingSummaryResponse(
    double averageStars, long ratingCount, List<RatingResponse> recent) {}
